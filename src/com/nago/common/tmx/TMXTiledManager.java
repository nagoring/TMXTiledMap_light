/*
 * Copyright (c) 2010-2011 e3roid project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * * Neither the name of the project nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package com.nago.common.tmx;

import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.Attributes;

import com.e3roid.drawable.sprite.TiledSprite;
import com.e3roid.drawable.tmx.TMXObjectGroup;
import com.e3roid.drawable.tmx.TMXProperty;
import com.e3roid.util.Debug;
import com.e3roid.util.SAXUtil;

//old name TMXTiledMap
public class TMXTiledManager {

	private static final String ORIENTATION = "orientation";
	private static final String WIDTH  = "width";
	private static final String HEIGHT = "height";
	private static final String TILE_WIDTH   = "tilewidth";
	private static final String TILE_HEIGHT  = "tileheight";
	
	private final String orientation;
	private final int columns;
	private final int rows;
	private final int tileWidth;
	private final int tileHeight;
	
	private boolean removed = false;
	
	private ArrayList<TMXLayerEx>   layers   = new ArrayList<TMXLayerEx>();
	private ArrayList<TMXTileSetEx> tileSets = new ArrayList<TMXTileSetEx>();
	private ArrayList<TMXObjectGroup> objectGroups = new ArrayList<TMXObjectGroup>();
	private HashMap<Integer, Integer> tileSetPropertyCache = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> spriteCache = new HashMap<Integer, Integer>();
	
	
	public TMXTiledManager(Attributes atts) {
		this.orientation = SAXUtil.getString(atts, ORIENTATION);
		this.columns = SAXUtil.getInt(atts, WIDTH);
		this.rows    = SAXUtil.getInt(atts, HEIGHT);
		this.tileWidth   = SAXUtil.getInt(atts, TILE_WIDTH);
		this.tileHeight  = SAXUtil.getInt(atts, TILE_HEIGHT);
	}
	
	public String getOrientation() {
		return this.orientation;
	}
	
	public int getColumns() {
		return this.columns;
	}
	
	public int getRows() {
		return this.rows;
	}
	
	public int getTileWidth() {
		return this.tileWidth;
	}
	
	public int getTileHeight() {
		return this.tileHeight;
	}
	
	
	public ArrayList<TMXLayerEx> getLayers() {
		return layers;
	}
	
	public void addTMXLayer(TMXLayerEx layer) {
		layers.add(layer);
	}
	
	public void removeTMXLayer(TMXLayerEx layer) {
		layers.remove(layer);
	}

//	public ArrayList<TiledMapData> getTieldMapDataList() {
//		return tiledMapDataList;
//	}
//	
//	public void addTieldMap(TiledMapData layer) {
//		tiledMapDataList.add(layer);
//	}
//	
//	public void removeTiledMapData(TiledMapData layer) {
//		tiledMapDataList.remove(layer);
//	}
	
	public ArrayList<TMXTileSetEx> getTileSets() {
		return tileSets;
	}
	
	public void addTileSet(TMXTileSetEx tileSet) {
		tileSets.add(tileSet);
	}
	
	public void removeTileSet(TMXTileSetEx tileSet) {
		tileSets.remove(tileSet);
	}
	
	public void addObjectGroup(TMXObjectGroup group) {
		objectGroups.add(group);
	}
	
	public ArrayList<TMXObjectGroup> getObjectGroups() {
		return objectGroups;
	}
	
	public ArrayList<TMXProperty> getTileProperties(int gid) {
		if (tileSetPropertyCache.containsKey(gid)) {
			int index = tileSetPropertyCache.get(gid);
			return tileSets.get(index).getTileProperty(gid);
		}
		for (int i = 0; i < tileSets.size(); i++) {
			TMXTileSetEx tileSet = tileSets.get(i);
			ArrayList<TMXProperty> props = tileSet.getTileProperty(gid);
			if (props != null) {
				tileSetPropertyCache.put(gid, i);
				return props;
			}
		}
		return null;
	}
	
	public TiledSprite getSpriteByGID(int gid) {
		if (removed) return null;
		try {
			if (spriteCache.containsKey(gid)) {
				int index = spriteCache.get(gid);
				return tileSets.get(index).getSprite(gid);
			}
			for (int i = tileSets.size() - 1; i >= 0; i--) {
				TMXTileSetEx tileSet = tileSets.get(i);
				if(gid >= tileSet.getFirstGID()) {
					TiledSprite sprite = tileSet.getSprite(gid);
					spriteCache.put(gid, i);
					return sprite;
				}
			}
		} catch (Exception e) {
			Debug.e(e);
		}
		return null;
	}

	public void onDispose() {
		removed = true;
	}
	
	public void onRemove() {
		removed = true;
		this.onDispose();
	}
}
