(function(root){
  root.ManoeuvreLayer = function(map, roadLayer, selectedManoeuvre, manoeuvresCollection) {
    var layerName = 'manoeuvre';
    Layer.call(this, layerName);
    var me = this;
    var manoeuvreSourceLookup = {
      0: { strokeColor: '#a4a4a2' },
      1: { strokeColor: '#0000ff' }
    };
    var featureTypeLookup = {
      normal: { strokeWidth: 8},
      overlay: { strokeColor: '#be0000', strokeLinecap: 'square', strokeWidth: 6, strokeDashstyle: '1 10'  }
    };
    var defaultStyleMap = new OpenLayers.StyleMap({
      'default': new OpenLayers.Style(OpenLayers.Util.applyDefaults({  strokeOpacity: 0.65  }))
    });
    defaultStyleMap.addUniqueValueRules('default', 'manoeuvreSource', manoeuvreSourceLookup);
    defaultStyleMap.addUniqueValueRules('default', 'type', featureTypeLookup);
    roadLayer.setLayerSpecificStyleMap(layerName, defaultStyleMap);

    var selectionStyleMap = new OpenLayers.StyleMap({
      'select':  new OpenLayers.Style(OpenLayers.Util.applyDefaults({ strokeOpacity: 0.9 })),
      'default': new OpenLayers.Style(OpenLayers.Util.applyDefaults({ strokeOpacity: 0.3 }))
    });
    selectionStyleMap.addUniqueValueRules('default', 'manoeuvreSource', manoeuvreSourceLookup);
    selectionStyleMap.addUniqueValueRules('select', 'manoeuvreSource', manoeuvreSourceLookup);
    selectionStyleMap.addUniqueValueRules('default', 'type', featureTypeLookup);
    selectionStyleMap.addUniqueValueRules('select', 'type', featureTypeLookup);

    var unselectManoeuvre = function() {
      selectedManoeuvre.close();
      roadLayer.setLayerSpecificStyleMap(layerName, defaultStyleMap);
      roadLayer.redraw();
      highlightFeatures(null);
      highlightOverlayFeatures([]);
    };

    var selectControl = new OpenLayers.Control.SelectFeature(roadLayer.layer, {
      onSelect: function(feature) {
        selectedManoeuvre.open(feature.attributes.roadLinkId);
        roadLayer.setLayerSpecificStyleMap(layerName, selectionStyleMap);
        roadLayer.redraw();
        highlightFeatures(feature.attributes.roadLinkId);
        highlightOverlayFeatures(manoeuvresCollection.getDestinationRoadLinksBySourceRoadLink(feature.attributes.roadLinkId));
      },
      onUnselect: function() {
        unselectManoeuvre();
      }
    });
    this.selectControl = selectControl;
    map.addControl(selectControl);

    var highlightFeatures = function(roadLinkId) {
      _.each(roadLayer.layer.features, function(x) {
        if (x.attributes.type === 'normal') {
          if (roadLinkId && (x.attributes.roadLinkId === roadLinkId)) {
            selectControl.highlight(x);
          } else {
            selectControl.unhighlight(x);
          }
        }
      });
    };

    var highlightOverlayFeatures = function(roadLinkIds) {
      _.each(roadLayer.layer.features, function(x) {
        if (x.attributes.type === 'overlay') {
          if (_.contains(roadLinkIds, x.attributes.roadLinkId)) {
            selectControl.highlight(x);
          } else {
            selectControl.unhighlight(x);
          }
        }
      });
    };

    var createDashedLineFeatures = function(roadLinks) {
      return _.flatten(_.map(roadLinks, function(roadLink) {
        var points = _.map(roadLink.points, function(point) {
          return new OpenLayers.Geometry.Point(point.x, point.y);
        });
        var attributes = _.merge({}, roadLink, {
          type: 'overlay'
        });
        return new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString(points), attributes);
      }));
    };

    var drawDashedLineFeatures = function(roadLinks) {
      var dashedRoadLinks = _.filter(roadLinks, function(roadLink) {
        return !_.isEmpty(roadLink.destinationOfManoeuvres);
      });
      roadLayer.layer.addFeatures(createDashedLineFeatures(dashedRoadLinks));
    };

    var reselectManoeuvre = function() {
      selectControl.activate();
      var originalOnSelectHandler = selectControl.onSelect;
      selectControl.onSelect = function() {};
      if (selectedManoeuvre.exists()) {
        var feature = _.find(roadLayer.layer.features, function(feature) {
          return feature.attributes.roadLinkId === selectedManoeuvre.getRoadLinkId();
        });
        if (feature) {
          selectControl.select(feature);
        }
        highlightOverlayFeatures(manoeuvresCollection.getDestinationRoadLinksBySourceRoadLink(selectedManoeuvre.getRoadLinkId()));
      }
      selectControl.onSelect = originalOnSelectHandler;
    };

    var draw = function() {
      selectControl.deactivate();
      roadLayer.drawRoadLinks(manoeuvresCollection.getAll(), map.getZoom());
      drawDashedLineFeatures(manoeuvresCollection.getAll());
      reselectManoeuvre();
    };

    this.refreshView = function() {
      manoeuvresCollection.fetch(map.getExtent(), map.getZoom(), draw);
    };

    var show = function(map) {
      if (zoomlevels.isInRoadLinkZoomLevel(map.getZoom())) {
        me.start();
      }
    };

    var hide = function() {
      unselectManoeuvre();
      me.stop();
    };

    return {
      show: show,
      hide: hide,
      minZoomForContent: zoomlevels.minZoomForRoadLinks
    };
  };
})(this);