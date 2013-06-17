package com.nago;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.os.AsyncTask;

import com.e3roid.E3Activity;
import com.e3roid.E3Engine;
import com.e3roid.E3Scene;
import com.e3roid.drawable.Sprite;
import com.e3roid.drawable.controls.DigitalController;
import com.e3roid.drawable.controls.StickController;
import com.e3roid.drawable.modifier.AlphaModifier;
import com.e3roid.drawable.modifier.SpanModifier;
import com.e3roid.drawable.sprite.AnimatedSprite;
import com.e3roid.drawable.sprite.TextSprite;
import com.e3roid.drawable.texture.AssetTexture;
import com.e3roid.drawable.texture.TiledTexture;
import com.e3roid.drawable.tmx.TMXException;
import com.e3roid.drawable.tmx.TMXTile;
import com.e3roid.event.ControllerEventListener;
import com.e3roid.util.Debug;
import com.e3roid.util.FPSListener;
import com.mk.common.drawable.tiledmap.ITiledMapData;
import com.mk.common.drawable.tiledmap.TiledMap;
import com.nago.common.tmx.TMXLayerEx;
import com.nago.common.tmx.TMXTiledManager;
import com.nago.common.tmx.TMXTiledMapLoaderEx;


public class TMXTiled_NagoActivity extends E3Activity implements ControllerEventListener ,FPSListener{ 
	private final static int WIDTH  = 640;
	private final static int HEIGHT = 480;
	private final static int VIEW_HEIGHT = 0;
	
	private TiledTexture texture;
	private AnimatedSprite sprite;
	private AssetTexture controlBaseTexture;
	private AssetTexture controlKnobTexture;
	private DigitalController controller; 
	
	private ArrayList<AnimatedSprite.Frame> downFrames  = new ArrayList<AnimatedSprite.Frame>();
	
	private TMXTiledManager manager;
	private ArrayList<TMXLayerEx> mapLayers;
	private TMXLayerEx collisionLayer;
	private TextSprite label;

    private TiledMap tiledMap;
    private TextSprite msprtextFPS;    
    
	@Override
	public E3Engine onLoadEngine() {
		E3Engine engine = new E3Engine(this, WIDTH, HEIGHT - VIEW_HEIGHT, E3Engine.RESOLUTION_FIXED_RATIO);
		engine.requestFullScreen();
		engine.requestLandscape();
        engine.getFPSCounter().addListener(this);
		return engine;
	}
    @Override
    public void onFPS(float fps, float min, float max) {
    	BigDecimal bi = new BigDecimal(String.valueOf(fps));
        msprtextFPS.setText("fps:" +  bi.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue() );
    }
	@Override
	public E3Scene onLoadScene() {
		
		final E3Scene scene = new E3Scene();

		int centerX = (getWidth()  - texture.getTileWidth())  / 2;
		int centerY = (getHeight() - texture.getTileHeight()) / 2;
		
		sprite = new AnimatedSprite(texture, centerX, centerY) {
			@Override
			public Rect getCollisionRect() {
				// king's collision rectangle is just around his body.
				Rect rect = this.getRect();
				rect.left   = rect.left   + this.getWidth() / 3;
				rect.right  = rect.right  - this.getWidth() / 3;
				rect.top    = rect.top    + this.getHeight() / 3;
				rect.bottom = rect.bottom - this.getHeight() / 3;
				return rect;
			}
		};
		sprite.animate(500, downFrames);
		
		// add digital controller
		controller = new DigitalController(
				controlBaseTexture, controlKnobTexture,
				0, getHeight() - controlBaseTexture.getHeight(), scene, this);
		controller.setAlpha(0.7f);
		controller.setUpdateInterval(10);
		scene.addEventListener(controller);
		
		// add loading text with blink modifier
		final TextSprite loadingText = new TextSprite("Loading map...", 24, this);
		loadingText.move((getWidth()  - loadingText.getWidth())  / 2,
						 (getHeight() - loadingText.getHeight()) / 2);
		loadingText.addModifier(new SpanModifier(500L, new AlphaModifier(0, 0, 1)));
		scene.getTopLayer().add(loadingText);
		
		// limit refresh rate while loading for saving cpu power
		engine.setRefreshMode(E3Engine.REFRESH_LIMITED);
		engine.setPreferredFPS(10);
		
		// loading maps could take some time so work in background.
		new AsyncTask<Void,Integer,TMXTiledManager>() {
			@Override
			protected TMXTiledManager doInBackground(Void... params) {
				// get the map from TMX map file.
				try {
					TMXTiledMapLoaderEx mapLoader = new TMXTiledMapLoaderEx();
					TMXTiledManager manager = mapLoader.loadFromAsset("desert.tmx", TMXTiled_NagoActivity.this);
					return manager;
				} catch (TMXException e) {
					Debug.e(e.getMessage());
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(TMXTiledManager tmxTiledMap) {
			    manager = tmxTiledMap;
				if (tmxTiledMap != null && (mapLayers = manager.getLayers()) != null) {
			        List<ITiledMapData> mapDataList = new ArrayList<ITiledMapData>();
					for (TMXLayerEx layer : mapLayers) {
						
						// Determine scene size of the layer.
						// This enables layer to skip drawing the tile which is out of the screen.
						layer.setSceneSize(getWidth(), getHeight());

						// if ground scrolls, child sprite follows it.
						if ("Ground".equals(layer.getName())) {
							mapDataList.add( layer.getTiledMapData() );
					        tiledMap = new TiledMap(mapDataList, getWidth(), getHeight(), false);
							tiledMap.setSceneSize(getWidth(), getHeight());
					        tiledMap.setRotationCenter(0, 0);
							tiledMap.addChild(sprite);
					        tiledMap.addChild(label);
						}
						
						
						// "Collision" layer is not to be added to the scene
						// because this layer is just defining collision of the tiles.
						if ("Collision".equals(layer.getName())) {
							collisionLayer = layer;
							continue;
						}
						
						scene.getTopLayer().remove(loadingText);
						
						// Add sprite to the scene.
						//追加した順番で優先順位が決まる。addした順に後ろにいく
						tiledMap.setPosition(0, 0);
						scene.getTopLayer().add(tiledMap);
						scene.getTopLayer().add(msprtextFPS);
						
						scene.addHUD(controller);
						
						// restore refresh rate to default
						engine.setRefreshMode(E3Engine.REFRESH_DEFAULT);
					}
				} else {
					loadingText.setText("Failed to load!");
					loadingText.setAlpha(1);
					loadingText.clearModifier();
				}
			}
		}.execute();
		scene.setBackgroundColor(0f, 0f, 0f, 1);

		label.move((getWidth() - label.getWidth()) / 2, (getHeight() - label.getHeight()) / 2);
		msprtextFPS.move( 10, 10 );
		return scene;
	}
	@Override
	public void onLoadResources() {
		texture = new TiledTexture("king.png", 31, 49, 0, 0, 3, 2, this);
		msprtextFPS =  new TextSprite("FPS", 40, this);

		// Initialize animation frames from tile.
		downFrames = new ArrayList<AnimatedSprite.Frame>();
		downFrames.add(new AnimatedSprite.Frame(0, 0));
		downFrames.add(new AnimatedSprite.Frame(1, 0));
		downFrames.add(new AnimatedSprite.Frame(2, 0));
		downFrames.add(new AnimatedSprite.Frame(3, 0));
		
		controlBaseTexture = new AssetTexture("controller_base.png", this);
		controlKnobTexture = new AssetTexture("controller_knob.png", this);
		
		label = new TextSprite("Hello, World!", 18, this);
		
	}
	/**
	 * @relativeX max 100
	 * @relativeY max 100
	 */
	@Override
	public void onControlUpdate(StickController controller,
			int relativeX, int relativeY, boolean hasChanged) {
		int dir = controller.getDirection();
		if (hasChanged) {
			if (dir == StickController.LEFT) {
				sprite.animate(500, downFrames);
			} else if (dir == StickController.RIGHT) {
				sprite.animate(500, downFrames);
			} else if (dir == StickController.UP) {
				sprite.animate(500, downFrames);
			} else if (dir == StickController.DOWN) {
				sprite.animate(500, downFrames);
			}
		}
		
		int xstep = (relativeX / 30);
		int ystep = (relativeY / 30);

		// move the sprite
		if (!collidesWithTile(sprite, xstep, ystep) && isInTheScene(sprite, xstep, ystep)) {
			sprite.moveRelative(xstep, ystep);
		}

		
	}
	private boolean isInTheScene(Sprite sprite, int xstep, int ystep) {
		int x = sprite.getRealX() + xstep;
		int y = sprite.getRealY() + ystep;
		return x > 0 && y > 0 && x < getWidth()  - sprite.getWidth() &&
				y < getHeight() - sprite.getHeight();
	}	
	
	private boolean collidesWithTile(AnimatedSprite sprite, int xstep, int ystep) {
		if (collisionLayer == null) return false;
		
		int positionCorrectionY = 4;
		ArrayList<TMXTile> tiles = collisionLayer.getTileFromRect(sprite.getCollisionRect(), xstep, ystep + positionCorrectionY); 
		int size = tiles.size();
		
		return size != 0;
	}

}
