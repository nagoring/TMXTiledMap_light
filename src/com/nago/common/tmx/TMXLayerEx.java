package com.nago.common.tmx;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.xml.sax.Attributes;

import android.graphics.Rect;

import com.e3roid.drawable.Shape;
import com.e3roid.drawable.sprite.TiledSprite;
import com.e3roid.drawable.texture.TiledTexture;
import com.e3roid.drawable.tmx.TMXTile;
import com.e3roid.util.Base64;
import com.e3roid.util.Base64InputStream;
import com.e3roid.util.Debug;
import com.e3roid.util.SAXUtil;
import com.mk.common.drawable.tiledmap.TiledMapData;

public class TMXLayerEx {
	private final String name;
	private final int columns;
	private final int rows;
	private final TMXTiledManager tiledMap;
	private final TMXTile[][] tiles;

	private int tileCount = 0;
	private boolean useLoop = false;
	private boolean stopOnTheEdge = true;
	
	private int x = 0;
	private int y = 0;
	
	private int width;
	private int height;
	private int sceneWidth;
	private int sceneHeight;
	
	
	private TiledMapData tiledMapData;
	
	
	private ArrayList<Shape> children = new ArrayList<Shape>();

	public TMXLayerEx(TMXTiledManager tiledMap, Attributes atts) {
		this.tiledMap = tiledMap;
		this.name = SAXUtil.getString(atts, "name");
		this.columns = SAXUtil.getInt(atts, "width");
		this.rows    = SAXUtil.getInt(atts, "height");
		this.tiles = new TMXTile[rows][columns];
		
	}
	
	public void addChild(Shape shape) {
		children.add(shape);
	}
	
	public void removeChild(Shape shape) {
		children.remove(shape);
	}

	public int setX(int x) {
		if (!useLoop && stopOnTheEdge) {
			if (x < 0) x = 0;
			if (x > getMaxX()) x = getMaxX();
		}
		if (x % width == 0) x = 0;
		this.x = x;
		return this.x;
	}
	
	public int setY(int y) {
		if (!useLoop && stopOnTheEdge) {
			if (y < 0) y = 0;
			if (y > getMaxY()) y = getMaxY();
		}
		if (y % height == 0) y = 0;
		this.y = y;
		return y;
	}
	
	public void setPosition(int x, int y) {
		this.setX(x);
		this.setY(y);
	}
	
	public void scroll(int x, int y) {
		int relativeX = this.x - setX(x);
		int relativeY = this.y - setY(y);
		
		for (Shape child : children) {
			child.moveRelative(relativeX, relativeY);
		}
	}
	
	public void setSceneSize(int width, int height) {
		this.sceneWidth  = width;
		this.sceneHeight = height;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}

	public int getColumnAtPosition(int x) {
		x = x % width;
		return x / (width / columns);
	}
	
	public int getRowAtPosition(int y) {
		y = y % height;
		return y / (height / rows);
	}
	
	public TMXTile getTileAt(int column, int row) {
		if(this.rows <= row || row < 0) return null;
		if(this.columns <= column || column < 0 ) return null;
		if (row >= tiles.length) return null;
		if (column >= tiles[row].length) return null;
		return tiles[row][column];
	}
	
	public TMXTile getTileFromPosition(int x, int y) {
		int column = getColumnAtPosition(x + this.x);
		int row = getRowAtPosition(y + this.y);
		return getTileAt(column, row);
	}
	
	public ArrayList<TMXTile> getTileFromRect(Rect rect) {
		return getTileFromRect(rect, 0, 0);
	}
	
	public ArrayList<TMXTile> getTileFromRect(Rect rect, int xstep, int ystep) {
		ArrayList<TMXTile> tiles = new ArrayList<TMXTile>();
		
		rect.left   += xstep;
		rect.right  += xstep;
		rect.top    += ystep;
		rect.bottom += ystep;
		
		TMXTile leftTop     = getTileFromPosition(rect.left,  rect.top);
		TMXTile leftBottom  = getTileFromPosition(rect.left,  rect.bottom);
		TMXTile rightTop    = getTileFromPosition(rect.right, rect.top);
		TMXTile rightBottom = getTileFromPosition(rect.right, rect.bottom);
		
		if (!TMXTile.isEmpty(leftTop)) tiles.add(leftTop);
		if (!TMXTile.isEmpty(leftBottom)  && !tiles.contains(leftBottom))  tiles.add(leftBottom);
		if (!TMXTile.isEmpty(rightTop)    && !tiles.contains(rightTop))    tiles.add(rightTop);
		if (!TMXTile.isEmpty(rightBottom) && !tiles.contains(rightBottom)) tiles.add(rightBottom);
		
		return tiles;
	}
	
	
	public String getName() {
		return this.name;
	}
	
	public int getColumns() {
		return this.columns;
	}
	
	public int getRows() {
		return this.rows;
	}
	
	public void extract(String data, String encoding, String compression) throws IOException {
		DataInputStream dataIn = null;
		try{
			InputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));

			if(encoding != null && encoding.equals("base64")) {
				in = new Base64InputStream(in, Base64.DEFAULT);
			}
			if(compression != null){
				if(compression.equals("gzip")) {
					in = new GZIPInputStream(in);
				} else {
					throw new IllegalArgumentException("compression '" + compression + "' is not supported.");
				}
			}
			dataIn = new DataInputStream(in);

			int expectedTileCount = columns * rows;
			while(this.tileCount < expectedTileCount) {
				this.addTile(readGID(dataIn));
			}
		} finally {
			try {
				dataIn.close();
			} catch (IOException e) {
				Debug.e(e);
			}
		}
	}
	
	private void addTile(int gid) {
		int column = tileCount % columns;
		int row    = tileCount / columns;

		if (gid == 0) {
			tiles[row][column] = TMXTile.getEmptyTile(column, row);
		} else {
			TiledSprite sprite = tiledMap.getSpriteByGID(gid);
			if (sprite == null) return;
			tiles[row][column] = new TMXTile(gid, column, row, 
					sprite.getTileIndexX(), sprite.getTileIndexY(), 
					sprite.getWidth(), sprite.getHeight());
			
			this.width  = columns * sprite.getWidth();
			this.height = rows * sprite.getHeight();
		}
		tiledMapData.setMapValue(column, row, gid - 1);
		
		this.tileCount++;
	}
	
	private int readGID(DataInputStream dataIn) throws IOException {
		int lowestByte = dataIn.read();
		int secondLowestByte  = dataIn.read();
		int secondHighestByte = dataIn.read();
		int highestByte = dataIn.read();

		if(lowestByte < 0 || secondLowestByte < 0 || secondHighestByte < 0 || highestByte < 0) {
			throw new IllegalArgumentException("Couldn't read gid from stream.");
		}

		return lowestByte | secondLowestByte <<  8 |secondHighestByte << 16 | highestByte << 24;
	}
	
	public void setup(Attributes atts) {
		this.addTile(SAXUtil.getInt(atts, "gid"));
	}

	
	public int getTileWidth() {
		return tiledMap.getTileWidth();
	}
	
	public int getTileHeight() {
		return tiledMap.getTileHeight();
	}
	
	public boolean useLoop() {
		return this.useLoop;
	}
	
	public void loop(boolean useLoop) {
		this.useLoop = useLoop;
	}
	
	public void stopOnTheEdge(boolean stop) {
		this.stopOnTheEdge = stop;
	}
	
	public int getX() {
		return this.x;
	}
	
	public int getY() {
		return this.y;
	}
	
	public int getSceneWidth() {
		return this.sceneWidth;
	}
	
	public int getSceneHeight() {
		return this.sceneHeight;
	}
	
	public int getMaxX() {
		return this.width - this.sceneWidth;
	}
	
	public int getMaxY() {
		return this.height - this.sceneHeight;
	}
	public TiledMapData getTiledMapData(){
		return tiledMapData;
	}
	public void setTiledMapData(TiledMapData tiledMapData){
		this.tiledMapData = tiledMapData;
	}
	public TiledTexture getTiledTexture(){
		return tiledMapData.getTiledTexture();
	}

}
