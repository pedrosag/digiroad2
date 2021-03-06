(function(root) {
  root.TileMapCollection = function(map) {
    var mapConfig = {
      tileSize: new OpenLayers.Size(256, 256),
      buffer: 0,
      requestEncoding: 'REST',
      matrixSet: 'ETRS-TM35FIN',
      style: 'default',
      tileOrigin: new OpenLayers.LonLat(-548576, 8388608),
      matrixIds: [
        { identifier: '0', scaleDenominator: 29257142.85714286 },
        { identifier: '1', scaleDenominator: 14628571.42857143 },
        { identifier: '2', scaleDenominator: 7314285.714285715 },
        { identifier: '3', scaleDenominator: 3657142.8571428573 },
        { identifier: '4', scaleDenominator: 1828571.4285714286 },
        { identifier: '5', scaleDenominator: 914285.7142857143 },
        { identifier: '6', scaleDenominator: 457142.85714285716 },
        { identifier: '7', scaleDenominator: 228571.42857142858 },
        { identifier: '8', scaleDenominator: 114285.71428571429 },
        { identifier: '9', scaleDenominator: 57142.857142857145 },
        { identifier: '10', scaleDenominator: 28571.428571428572 },
        { identifier: '11', scaleDenominator: 14285.714285714286 },
        { identifier: '12', scaleDenominator: 7142.857142857143 },
        { identifier: '13', scaleDenominator: 3571.4285714285716 },
        { identifier: '14', scaleDenominator: 1785.7142857142858 }
      ]
    };

    var aerialMapConfig = _.merge({}, mapConfig, {
      url: 'maasto/wmts/1.0.0/ortokuva/default/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.jpg',
      layer: 'aerialmap',
      format: 'image/jpeg',
      serverResolutions: [2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 0.5]
    });

    var backgroundMapConfig = _.merge({}, mapConfig, {
      url: 'maasto/wmts/1.0.0/taustakartta/default/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.png',
      layer: 'backgroundmap',
      format: 'image/png',
      serverResolutions: [2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 0.5]
    });

    var terrainMapConfig = _.merge({}, mapConfig, {
      url: 'maasto/wmts/1.0.0/maastokartta/default/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.png',
      layer: 'terrainmap',
      format: 'image/png',
      serverResolutions: [2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 0.5]
    });

    var backgroundMapLayer = new OpenLayers.Layer.WMTS(backgroundMapConfig);
    var aerialMapLayer = new OpenLayers.Layer.WMTS(aerialMapConfig);
    var terrainMapLayer = new OpenLayers.Layer.WMTS(terrainMapConfig);
    var tileMapLayers = {
      background: backgroundMapLayer,
      aerial: aerialMapLayer,
      terrain: terrainMapLayer
    };

    var selectMap = function(tileMap) {
      _.forEach(tileMapLayers, function(layer, key) {
        if (key === tileMap) {
          layer.setVisibility(true);
          map.setBaseLayer(layer);
        } else {
          layer.setVisibility(false);
        }
      });
    };

    map.addLayers([backgroundMapLayer, aerialMapLayer, terrainMapLayer]);
    selectMap('background');
    eventbus.on('tileMap:selected', selectMap);
  };
})(this);