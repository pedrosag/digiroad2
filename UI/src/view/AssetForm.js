(function(root) {
  root.AssetForm = {
    initialize: function(backend) {
      var enumeratedPropertyValues = null;
      var readOnly = true;
      var streetViewHandler;
      var activeLayer = 'asset';

      _.templateSettings = {
        interpolate: /\{\{(.+?)\}\}/g
      };

      var renderAssetForm = function() {
        if (activeLayer !== 'asset') {
          return;
        }
        var container = $("#feature-attributes").empty();

        var element = $('<header />').html(busStopHeader());
        var wrapper = $('<div />').addClass('wrapper');
        streetViewHandler = getStreetView();
        wrapper.append(streetViewHandler.render()).append($('<div />').addClass('form form-horizontal form-dark').attr('role', 'form').append(getAssetForm()));
        var featureAttributesElement = container.append(element).append(wrapper);
        addDatePickers();

        var cancelBtn = $('<button />').prop('disabled', !selectedAssetModel.isDirty()).addClass('cancel btn btn-secondary').text('Peruuta').click(function() {
          $("#feature-attributes").empty();
          selectedAssetModel.cancel();
        });

        var saveBtn = $('<button />').prop('disabled', !selectedAssetModel.isDirty()).addClass('save btn btn-primary').text('Tallenna').click(function() {
          selectedAssetModel.save();
        });

        eventbus.on('asset:moved assetPropertyValue:changed', function() {
          saveBtn.prop('disabled', false);
          cancelBtn.prop('disabled', false);
        }, this);

        // TODO: cleaner html
        featureAttributesElement.append($('<footer />').addClass('form-controls').append(saveBtn).append(cancelBtn));

        if (readOnly) {
          $('#feature-attributes .form-controls').hide();
          wrapper.addClass('read-only');
        }

        function busStopHeader(asset) {
          if (_.isNumber(selectedAssetModel.get('externalId'))) {
            return 'Valtakunnallinen ID: ' + selectedAssetModel.get('externalId');
          }
          else return 'Uusi pys&auml;kki';
        }
      };

      var getStreetView = function() {
        var model = selectedAssetModel;
        var render = function() {
          var wgs84 = OpenLayers.Projection.transform(
            new OpenLayers.Geometry.Point(model.get('lon'), model.get('lat')),
            new OpenLayers.Projection('EPSG:3067'), new OpenLayers.Projection('EPSG:4326'));
          return $(streetViewTemplate({
            wgs84X: wgs84.x,
            wgs84Y: wgs84.y,
            heading: (model.get('validityDirection') === 3 ? model.get('bearing') - 90 : model.get('bearing') + 90),
            protocol: location.protocol
          })).addClass('street-view');
        };

        var update = function(){
          $('.street-view').empty().append(render());
        };

        return {
          render: render,
          update: update
        };
      };

      var addDatePickers = function () {
        var $validFrom = $('#ensimmainen_voimassaolopaiva');
        var $validTo = $('#viimeinen_voimassaolopaiva');
        if ($validFrom.length > 0 && $validTo.length > 0) {
          dateutil.addDependentDatePickers($validFrom, $validTo);
        }
      };

      var readOnlyHandler = function(property){
        var outer = $('<div />').addClass('form-group');
        var propertyVal = _.isEmpty(property.values) === false ? property.values[0].propertyDisplayValue : '';
        // TODO: use cleaner html
        if (property.propertyType === 'read_only_text') {
          outer.append($('<p />').addClass('form-control-static asset-log-info').text(property.localizedName + ': ' + propertyVal));
        } else {
          outer.append($('<label />').addClass('control-label').text(property.localizedName));
          outer.append($('<p />').addClass('form-control-static').text(propertyVal));
        }
        return outer;
      };

      var textHandler = function(property){
        return createTextWrapper(property).append(createTextElement(readOnly, property));
      };

      var createTextWrapper = function(property) {
        var wrapper = $('<div />').addClass('form-group');
        wrapper.append($('<label />').addClass('control-label').text(property.localizedName));
        return wrapper;
      };

      var createTextElement = function(readOnly, property) {
        var element;
        var elementType;

        if (readOnly) {
          elementType = $('<p />').addClass('form-control-static');
          element = elementType;

          if (property.values[0]) {
            element.text(property.values[0].propertyDisplayValue);
          } else {
            element.addClass('undefined').html('Ei m&auml;&auml;ritetty');
          }
        } else {
          elementType = property.propertyType === 'long_text' ?
            $('<textarea />').addClass('form-control') : $('<input type="text"/>').addClass('form-control');
          element = elementType.bind('input', function(target){
            selectedAssetModel.setProperty(property.publicId, [{ propertyValue: target.currentTarget.value }]);
          });

          if(property.values[0]) {
            element.val(property.values[0].propertyDisplayValue);
          }
        }

        return element;
      };

      var singleChoiceHandler = function(property, choices){
        return createSingleChoiceWrapper(property).append(createSingleChoiceElement(readOnly, property, choices));
      };

      var createSingleChoiceWrapper = function(property) {
        wrapper = $('<div />').addClass('form-group');
        wrapper.append($('<label />').addClass('control-label').text(property.localizedName));
        return wrapper;
      };

      var createSingleChoiceElement = function(readOnly, property, choices) {
        var element;
        var enumValues = _.find(choices, function(choice){
          return choice.publicId === property.publicId;
        }).values;

        if (readOnly) {
          element = $('<p />').addClass('form-control-static');

          if (property.values && property.values[0]) {
            element.text(property.values[0].propertyDisplayValue);
          } else {
            element.html('Ei tiedossa');
          }
        } else {
          element = $('<select />').addClass('form-control').change(function(x){
            selectedAssetModel.setProperty(property.publicId, [{ propertyValue: x.currentTarget.value }]);
          });

          element = _.reduce(enumValues, function(element, value) {
            var option = $('<option>').text(value.propertyDisplayValue).attr('value', value.propertyValue);
            element.append(option);
            return element;
          }, element);

          if(property.values && property.values[0]) {
            element.val(property.values[0].propertyValue);
          } else {
            element.val('99');
          }
        }

        return element;
      };

      var directionChoiceHandler = function(property){
        if (!readOnly) {
          return createDirectionChoiceWrapper(property).append(createDirectionChoiceElement(readOnly, property));
        }
      };

      var createDirectionChoiceWrapper = function(property) {
        wrapper = $('<div />').addClass('form-group');
        wrapper.append($('<label />').addClass('control-label').text(property.localizedName));
        return wrapper;
      };

      var createDirectionChoiceElement = function(property) {
        var element;
        var wrapper;

        element = $('<button />').addClass('btn btn-secondary btn-block').text('Vaihda suuntaa').click(function(){
          selectedAssetModel.switchDirection();
          streetViewHandler.update();
        });

        if(property.values && property.values[0]) {
          validityDirection = property.values[0].propertyValue;
        }

        return element;
      };

      var dateHandler = function(property){
        return createDateWrapper(property).append(createDateElement(readOnly, property));
      };

      var createDateWrapper = function(property) {
        wrapper = $('<div />').addClass('form-group');
        wrapper.append($('<label />').addClass('control-label').text(property.localizedName));
        return wrapper;
      };

      var createDateElement = function(readOnly, property) {
        var element;
        var wrapper;

        if (readOnly) {
          element = $('<p />').addClass('form-control-static');

          if (property.values[0]) {
            element.text(dateutil.iso8601toFinnish(property.values[0].propertyDisplayValue));
          } else {
            element.addClass('undefined').html('Ei m&auml;&auml;ritetty');
          }
        } else {
          element = $('<input type="text"/>').addClass('form-control').attr('id', property.publicId).on('keyup datechange', _.debounce(function(target){
            // tab press
            if(target.keyCode === 9){
              return;
            }
            var propertyValue = _.isEmpty(target.currentTarget.value) ? '' : dateutil.finnishToIso8601(target.currentTarget.value);
            selectedAssetModel.setProperty(property.publicId, [{ propertyValue: propertyValue }]);
          }, 500));

          if(property.values[0]) {
            element.val(dateutil.iso8601toFinnish(property.values[0].propertyDisplayValue));
          }
        }

        return element;
      };


      var multiChoiceHandler = function(property, choices){
        return createMultiChoiceWrapper(property).append(createMultiChoiceElement(readOnly, property, choices));
      };

      var createMultiChoiceWrapper = function(property) {
        wrapper = $('<div />').addClass('form-group');
        wrapper.append($('<label />').addClass('control-label').text(property.localizedName));
        return wrapper;
      };

      var createMultiChoiceElement = function(readOnly, property, choices) {
        var element;
        var wrapper;
        var currentValue = _.cloneDeep(property);
        var enumValues = _.chain(choices)
          .filter(function(choice){
            return choice.publicId === property.publicId;
          })
          .flatten('values')
          .filter(function(x){
            return x.propertyValue !== '99';
          }).value();

        if (readOnly) {
          element = $('<ul />');
        } else {
          element = $('<div />');
        }

        element.addClass('choice-group');

        element = _.reduce(enumValues, function(element, value) {
          value.checked = _.any(currentValue.values, function (prop) {
            return prop.propertyValue === value.propertyValue;
          });

          if (readOnly) {
            if (value.checked) {
              var item = $('<li />');
              item.text(value.propertyDisplayValue);

              element.append(item);
            }
          } else {
            var container = $('<div class="checkbox" />');
            var input = $('<input type="checkbox" />').change(function (evt) {
              value.checked = evt.currentTarget.checked;
              var values = _.chain(enumValues)
                .filter(function (value) {
                  return value.checked;
                })
                .map(function (value) {
                  return { propertyValue: parseInt(value.propertyValue, 10) };
                })
                .value();
              if (_.isEmpty(values)) { values.push({ propertyValue: 99 }); }
              selectedAssetModel.setProperty(property.publicId, values);
            });

            input.prop('checked', value.checked);

            var label = $('<label />').text(value.propertyDisplayValue);
            element.append(container.append(label.append(input)));
          }

          return element;
        }, element);

        return element;
      };

      var getAssetForm = function() {
        var contents = selectedAssetModel.getProperties();
        var components =_.map(contents, function(feature){
          feature.localizedName = window.localizedStrings[feature.publicId];
          var propertyType = feature.propertyType;
          if (propertyType === "text" || propertyType === "long_text") {
            return textHandler(feature);
          } else if (propertyType === "read_only_text" || propertyType === 'read-only') {
            return readOnlyHandler(feature);
          } else if (feature.publicId === 'vaikutussuunta') {
            return directionChoiceHandler(feature);
          } else if (propertyType === "single_choice") {
            return singleChoiceHandler(feature, enumeratedPropertyValues);
          } else if (feature.propertyType === "multiple_choice") {
            return multiChoiceHandler(feature, enumeratedPropertyValues);
          } else if (propertyType === "date") {
            return dateHandler(feature);
          }  else {
            feature.propertyValue = 'Ei toteutettu';
            return $(featureDataTemplateNA(feature));
          }
        });

        return $('<div />').append(components);
      };

      var streetViewTemplate  = _.template(
          '<a target="_blank" href="{{protocol}}//maps.google.com/?ll={{wgs84Y}},{{wgs84X}}&cbll={{wgs84Y}},{{wgs84X}}&cbp=12,{{heading}}.09,,0,5&layer=c&t=m">' +
          '<img alt="Google StreetView-n&auml;kym&auml;" src="http://maps.googleapis.com/maps/api/streetview?key=AIzaSyBh5EvtzXZ1vVLLyJ4kxKhVRhNAq-_eobY&size=360x180&location={{wgs84Y}}' +
          ', {{wgs84X}}&fov=110&heading={{heading}}&pitch=-10&sensor=false">' +
          '</a>');

      var featureDataTemplateNA = _.template('<div class="formAttributeContentRow">' +
        '<div class="formLabels">{{localizedName}}</div>' +
        '<div class="featureAttributeNA">{{propertyValue}}</div>' +
        '</div>');

      var closeAsset = function() {
        $("#feature-attributes").html('');
        dateutil.removeDatePickersFromDom();
      };

      eventbus.on('asset:modified', function(){
        renderAssetForm();
      });

      eventbus.on('layer:selected', function(layer) {
        activeLayer = layer;
        closeAsset();
      });

      eventbus.on('application:readOnly', function(data) {
        readOnly = data;
      });

      eventbus.on('asset:closed', closeAsset);

      eventbus.on('enumeratedPropertyValues:fetched', function(values) {
        enumeratedPropertyValues = values;
      });

      eventbus.on('asset:moved', function() {
        streetViewHandler.update();
      });

      backend.getEnumeratedPropertyValues(10);
    }
  };
})(this);
