package com.nago.common.tmx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;

import com.e3roid.drawable.texture.TiledTexture;
import com.e3roid.drawable.tmx.TMXException;
import com.e3roid.drawable.tmx.TMXObject;
import com.e3roid.drawable.tmx.TMXObjectGroup;
import com.e3roid.drawable.tmx.TMXProperty;
import com.e3roid.util.SAXUtil;
import com.mk.common.drawable.tiledmap.TiledMapData;

public class TMXTiledMapLoaderEx extends DefaultHandler{
	private static final String TAG_DATA = "data";
	private static final String TAG_DATA_ATTRIBUTE_ENCODING = "encoding";
	private static final String TAG_DATA_ATTRIBUTE_COMPRESSION = "compression";
	private static final String TAG_IMAGE = "image";
	private static final String TAG_LAYER = "layer";
	private static final String TAG_MAP = "map";
	private static final String TAG_PROPERTY = "property";
	private static final String TAG_TILESET = "tileset";
	private static final String TAG_TILESET_ATTRIBUTE_SOURCE = "source";
	private static final String TAG_TILESET_ATTRIBUTE_FIRSTGID = "firstgid";
	private static final String TAG_TILE = "tile";
	private static final String TAG_TILE_ATTRIBUTE_ID = "id";
	private static final String TAG_IMAGE_ATTRIBUTE_SOURCE = "source";

	private static final String TAG_OBJECTGROUP = "objectgroup";
	private static final String TAG_OBJECT = "object";
	
	private StringBuilder characters = new StringBuilder();
	
	private Context context;
	private TMXTiledManager tmxTiledManager;
	private String encoding;
	private String compression;
	private int lastTileSetTileID;
	
	private boolean inTileset = false;
	private boolean inTile = false;
	private boolean inData = false;
	private boolean inObject = false;
	
	public TMXTiledManager loadFromAsset(String filename, Context context) throws TMXException {
		try {
			return load(context.getAssets().open(filename), context);
		} catch (IOException e) {
			throw new TMXException(e);
		}
	}
	
	public TMXTiledManager load(InputStream inputStream, Context context) throws TMXException {
		try {
			this.context = context;
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);

			xr.parse(new InputSource(new BufferedInputStream(inputStream)));
		} catch (Exception e) {
			throw new TMXException(e);
		}

		return this.tmxTiledManager;
	}
	
	@Override
	public void startElement(String uri, String localName, 
			String qName, Attributes atts) throws SAXException {
		if(localName.equals(TAG_MAP)){
			this.tmxTiledManager = new TMXTiledManager(atts);
		} else if(localName.equals(TAG_TILESET)){
			this.inTileset = true;
			String tsxTileSetSource = SAXUtil.getString(atts, TAG_TILESET_ATTRIBUTE_SOURCE);
			if(tsxTileSetSource == null) {
				this.tmxTiledManager.addTileSet(new TMXTileSetEx(
						SAXUtil.getInt(atts, TAG_TILESET_ATTRIBUTE_FIRSTGID), atts, context));
			}
		} else if(localName.equals(TAG_IMAGE)){
			ArrayList<TMXTileSetEx> tmxTileSets = this.tmxTiledManager.getTileSets();
			String imageSource = SAXUtil.getString(atts, TAG_IMAGE_ATTRIBUTE_SOURCE);
			tmxTileSets.get(tmxTileSets.size() - 1).setImageSource(imageSource);
			
			
			
		} else if(localName.equals(TAG_TILE)) {
			this.inTile = true;
			if(this.inTileset) {
				this.lastTileSetTileID = SAXUtil.getInt(atts, TAG_TILE_ATTRIBUTE_ID);
			} else if(this.inData) {
				ArrayList<TMXLayerEx> tmxLayers = this.tmxTiledManager.getLayers();
				tmxLayers.get(tmxLayers.size() - 1).setup(atts);
			}
		} else if(localName.equals(TAG_PROPERTY)) {
			if (this.inTile) {
				ArrayList<TMXTileSetEx> tmxTileSets = this.tmxTiledManager.getTileSets();
				tmxTileSets.get(tmxTileSets.size() - 1).addTileProperty(this.lastTileSetTileID, new TMXProperty(atts));
			} else if (this.inObject) {
				ArrayList<TMXObjectGroup> groups = this.tmxTiledManager.getObjectGroups();
				TMXObjectGroup lastGroup = groups.get(groups.size() - 1);

				ArrayList<TMXObject> objects = lastGroup.getObjects();
				objects.get(objects.size() - 1).addObjectProperty(new TMXProperty(atts));
			}
		} else if(localName.equals(TAG_LAYER)){
			//レイヤーの度に呼び出される
			TMXLayerEx tmpTMXLayer = new TMXLayerEx(this.tmxTiledManager, atts);
			int index = tmxTiledManager.getLayers().size();
			
			ArrayList<TMXTileSetEx> tmxTileSets = this.tmxTiledManager.getTileSets();
			TiledTexture tmpTexture = tmxTileSets.get(index).getTexture();
			TiledMapData tiledMapData = new TiledMapData(
					tmpTexture, tmpTMXLayer.getColumns(), tmpTMXLayer.getRows()
					);
			
			tmpTMXLayer.setTiledMapData(tiledMapData);
			this.tmxTiledManager.addTMXLayer(tmpTMXLayer);

			
		} else if(localName.equals(TAG_DATA)){
			this.inData = true;
			this.encoding    = SAXUtil.getString(atts, TAG_DATA_ATTRIBUTE_ENCODING);
			this.compression = SAXUtil.getString(atts, TAG_DATA_ATTRIBUTE_COMPRESSION);
		} else if (localName.equals(TAG_OBJECTGROUP)) {
			this.tmxTiledManager.addObjectGroup(new TMXObjectGroup(atts));
		} else if (localName.equals(TAG_OBJECT)) {
			this.inObject = true;
			ArrayList<TMXObjectGroup> groups = this.tmxTiledManager.getObjectGroups();
			groups.get(groups.size() - 1).addObject(new TMXObject(atts));
		}
	}
	

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if(localName.equals(TAG_TILESET)){
			this.inTileset = false;
		} else if(localName.equals(TAG_TILE)) {
			this.inTile = false;
		} else if(localName.equals(TAG_DATA)){
			boolean binarySaved = this.compression != null && this.encoding != null;
			if(binarySaved) {
				ArrayList<TMXLayerEx> tmxLayers = this.tmxTiledManager.getLayers();
				try {
					tmxLayers.get(tmxLayers.size() - 1).extract(this.characters.toString().trim(), this.encoding, this.compression);
				} catch (IOException e) {
					throw new SAXException(e);
				}
				this.compression = null;
				this.encoding = null;
			}
			this.inData = false;
		} else if (localName.equals(TAG_OBJECT)) {
			this.inObject = false;
		}

		this.characters.setLength(0);
	}
	
	@Override
	public void characters(char[] characters, int start, int length) throws SAXException {
		this.characters.append(characters, start, length);
	}

}
