define(['chai', 'SpeedLimitLayer', 'zoomlevels'], function(chai, SpeedLimitLayer) {
    var assert = chai.assert;

    describe('SpeedLimitLayer', function () {
        describe('when moving map', function() {
            var layer;
            before(function() {
                layer = new SpeedLimitLayer({
                    getSpeedLimits: function() {
                        eventbus.trigger('speedLimits:fetched', [
                            {id: 1, points: [{x: 0, y: 0}]},
                            {id: 2, points: [{x: 10, y: 10}]}
                        ]);
                    }
                });
                layer.update(9, null);
                eventbus.trigger('map:moved', {selectedLayer: 'speedLimit', bbox: null, zoom: 9});
            });

            it('should contain each speed limit only once', function() {
                var getFirstPointOfFeature = function(feature) {
                    return feature.geometry.getVertices()[0];
                };

                assert.equal(layer.vectorLayer.features.length, 2);
                assert.equal(getFirstPointOfFeature(layer.vectorLayer.features[0]).x, 0);
                assert.equal(getFirstPointOfFeature(layer.vectorLayer.features[0]).y, 0);
                assert.equal(getFirstPointOfFeature(layer.vectorLayer.features[1]).x, 10);
                assert.equal(getFirstPointOfFeature(layer.vectorLayer.features[1]).y, 10);
            });

            describe('and zooming out', function() {
                before(function() {
                    eventbus.trigger('map:moved', {selectedLayer: 'speedLimit', bbox: null, zoom: 8});
                });

                it('should not contain speed limits', function() {
                    assert.equal(layer.vectorLayer.features.length, 0);
                });

                describe('and zooming in', function() {
                    before(function() {
                        eventbus.trigger('map:moved', {selectedLayer: 'speedLimit', bbox: null, zoom: 9});
                    });

                    it('should contain speed limits', function() {
                        assert.equal(layer.vectorLayer.features.length, 2);
                    });
                });
            });
        });
    });
});