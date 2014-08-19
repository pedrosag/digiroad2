(function(root) {
  root.ApplicationSetupTestData = {
    generate: function() {
      return {
        "startupSequence": [
          {
            "instanceProps": {},
            "title": "OpenLayers",
            "bundleinstancename": "openlayers-default-theme",
            "fi": "OpenLayers",
            "sv": "?",
            "en": "OpenLayers",
            "bundlename": "openlayers-default-theme",
            "metadata": {
              "Import-Bundle": {
                "openlayers-default-theme": {
                  "bundlePath": "../bower_components/oskari.org/packages/openlayers/bundle/"
                },
                "openlayers-single-full": {
                  "bundlePath": "../bower_components/oskari.org/packages/openlayers/bundle/"
                },
                "oskariui": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                }
              },
              "Require-Bundle-Instance": []
            }
          },
          {
            "instanceProps": {},
            "title": "Map",
            "bundleinstancename": "mapfull",
            "fi": "Map",
            "sv": "?",
            "en": "Map",
            "bundlename": "mapfull",
            "metadata": {
              "Import-Bundle": {
                "mapwmts": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "service-base": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "event-map-layer": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "request-map-layer": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "mapmodule-plugin": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "event-base": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "mapfull": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "core-base": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "request-base": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "domain": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "core-map": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "oskariui": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "request-map": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "sandbox-base": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "service-map": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "sandbox-map": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                },
                "event-map": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                }
              },
              "Require-Bundle-Instance": []
            }
          },
          {
            "instanceProps": {},
            "title": "Oskari DIV Manazer",
            "bundleinstancename": "divmanazer",
            "fi": "Oskari DIV Manazer",
            "sv": "?",
            "en": "Oskari DIV Manazer",
            "bundlename": "divmanazer",
            "metadata": {
              "Import-Bundle": {
                "divmanazer": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                }
              },
              "Require-Bundle-Instance": []
            }
          },
          {
            "instanceProps": {},
            "title": "StateHandler",
            "bundleinstancename": "statehandler",
            "fi": "jquery",
            "sv": "?",
            "en": "?",
            "bundlename": "statehandler",
            "metadata": {
              "Import-Bundle": {
                "statehandler": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                }
              },
              "Require-Bundle-Instance": []
            }
          },
          {
            "instanceProps": {},
            "bundleinstancename": "coordinatedisplay",
            "fi": "coordinatedisplay",
            "sv": "?",
            "en": "?",
            "bundlename": "coordinatedisplay",
            "metadata": {
              "Import-Bundle": {
                "coordinatedisplay": {
                  "bundlePath": "../bower_components/oskari.org/packages/framework/bundle/"
                }
              },
              "Require-Bundle-Instance": []
            }
          }
        ]
      };
    }
  };
}(this));
