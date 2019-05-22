package com.sx4.webserver.gif;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/** Modified version of
	<a href="http://elliot.kroo.net/software/java/GifSequenceWriter/GifSequenceWriter.java">GifSequenceWriter</a> 
	by <b>Elliot Kroo</b>
*/
public class GifWriter {
	
	protected ImageWriter gifWriter;
	protected ImageWriteParam imageWriteParam;
	protected IIOMetadata imageMetaData;
	
	protected ImageOutputStream imageOutputStream;
	
	/**
	 * Creates a new GifSequenceWriter
	 * 
	 * @param outputStream the ImageOutputStream to be written to
	 * @param imageType one of the imageTypes specified in BufferedImage
	 * @param timeBetweenFramesMS the time between frames in milliseconds
	 * @param loopContinuously whether the gif should loop repeatedly
	 * @throws IIOException if no gif ImageWriters are found
	 */
	public GifWriter(ImageOutputStream outputStream,
		int imageType,
		int timeBetweenFramesMS,
		boolean loopContinuously) throws IIOException, IOException {
		
		this.imageOutputStream = outputStream;
		
		this.gifWriter = getWriter();
		
		this.imageWriteParam = this.gifWriter.getDefaultWriteParam();
		ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
		
		this.imageMetaData = this.gifWriter.getDefaultImageMetadata(imageTypeSpecifier, this.imageWriteParam);
		
		String metaFormatName = this.imageMetaData.getNativeMetadataFormatName();
		
		IIOMetadataNode root = (IIOMetadataNode) this.imageMetaData.getAsTree(metaFormatName);
		
		IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
		graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
		graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
		
		graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS/10));
		
		graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
		graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");
		
		IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");
		
		IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
		child.setAttribute("applicationID", "NETSCAPE");
		child.setAttribute("authenticationCode", "2.0");
		
		int loop = loopContinuously ? 0 : 1;
		
		child.setUserObject(new byte[] { 0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF) });
		appEntensionsNode.appendChild(child);
		
		this.imageMetaData.setFromTree(metaFormatName, root);
		
		this.gifWriter.setOutput(outputStream);
		this.gifWriter.prepareWriteSequence(null);
	}
	
	/**
	 * Creates a new GifSequenceWriter
	 * 
	 * @param outputStream the OutputStream to be written to (Wrapped by a MemoryCacheImageOutputStream)
	 * @param imageType one of the imageTypes specified in BufferedImage
	 * @param timeBetweenFramesMS the time between frames in milliseconds
	 * @param loopContinuously whether the gif should loop repeatedly
	 * @throws IIOException if no gif ImageWriters are found
	 */
	public GifWriter(OutputStream outputStream, 
		int imageType, 
		int timeBetweenFramesMS, 
		boolean loopContinuously) throws IIOException, IOException {
		
		this(new MemoryCacheImageOutputStream(outputStream), imageType, timeBetweenFramesMS, loopContinuously);
	}
	
	/**
	 * Creates a new GifSequenceWriter
	 * 
	 * @param outputStream the ImageOutputStream to be written to
	 * @param metadata the metadata used when writing the images
	 * 
	 * @throws IIOException if no gif ImageWriters are found
	 */
	public GifWriter(ImageOutputStream outputStream, IIOMetadata metadata) throws IIOException, IOException {
		this.imageOutputStream = outputStream;
		
		this.gifWriter = getWriter();
		
		this.imageWriteParam = this.gifWriter.getDefaultWriteParam();
		
		this.imageMetaData = metadata;
		
		this.gifWriter.setOutput(outputStream);
		this.gifWriter.prepareWriteSequence(null);
	}
	
	/**
	 * Creates a new GifSequenceWriter
	 * 
	 * @param outputStream the ImageOutputStream to be written to
	 * 
	 * @throws IIOException if no gif ImageWriters are found
	 */
	public GifWriter(ImageOutputStream outputStream,
		int imageType,
		IIOMetadataNode graphicControl, 
		IIOMetadataNode applicationExtensions) throws IIOException, IOException {
		
		if(!graphicControl.getNodeName().equals("GraphicControlExtension")) {
			throw new IllegalArgumentException();
		}
		
		if(!applicationExtensions.getNodeName().equals("ApplicationExtensions")) {
			throw new IllegalArgumentException();
		}
		
		this.imageOutputStream = outputStream;
		
		this.gifWriter = getWriter();
		
		this.imageWriteParam = this.gifWriter.getDefaultWriteParam();
		ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
		
		this.imageMetaData = this.gifWriter.getDefaultImageMetadata(imageTypeSpecifier, this.imageWriteParam);
		
		String metaFormatName = this.imageMetaData.getNativeMetadataFormatName();
		
		IIOMetadataNode root = (IIOMetadataNode) this.imageMetaData.getAsTree(metaFormatName);
		
		insertNode(root, graphicControl);
		insertNode(root, applicationExtensions);
		
		this.imageMetaData.setFromTree(metaFormatName, root);
		
		this.gifWriter.setOutput(outputStream);
		this.gifWriter.prepareWriteSequence(null);
	}
	
	public GifWriter(OutputStream outputStream, int imageType, IIOMetadataNode graphicControl, IIOMetadataNode applicationExtensions) throws IIOException, IOException {
		this(new MemoryCacheImageOutputStream(outputStream), imageType, graphicControl, applicationExtensions);
	}
	
	/**
	 * Creates a new GifSequenceWriter
	 * 
	 * @param outputStream the OutputStream to be written to (Wrapped by a MemoryCacheImageOutputStream)
	 * @param metadata the metadata used when writing the images
	 * 
	 * @throws IIOException if no gif ImageWriters are found
	 */
	public GifWriter(OutputStream outputStream, IIOMetadata metadata) throws IIOException, IOException {
		this(new MemoryCacheImageOutputStream(outputStream), metadata);
	}
	
	public void write(RenderedImage image) throws IOException {
		this.gifWriter.writeToSequence(new IIOImage(image, null, this.imageMetaData), this.imageWriteParam);
	}
	
	/**
	 * Close this GifSequenceWriter object. This closes any underlying stream
	 */
	public void finish() throws IOException {
		this.gifWriter.endWriteSequence();
		this.imageOutputStream.close();
	}
	
	/**
	 * Returns the first available GIF ImageWriter using 
	 * ImageIO.getImageWritersBySuffix("gif").
	 * 
	 * @return a GIF ImageWriter object
	 * @throws IIOException if no GIF image writers are returned
	 */
	private static ImageWriter getWriter() throws IIOException {
		Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
		if(!iter.hasNext()) {
			throw new IIOException("No GIF Image Writers Exist");
		}else{
			return iter.next();
		}
	}
	
	/**
	 * Returns an existing child node, or creates and returns a new child node (if 
	 * the requested node does not exist).
	 * 
	 * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
	 * @param nodeName the name of the child node.
	 * 
	 * @return the child node, if found or a new node created with the given name.
	 */
	private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
		int nNodes = rootNode.getLength();
		for(int i = 0; i < nNodes; i++) {
			if(rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
				return ((IIOMetadataNode) rootNode.item(i));
			}
		}
		
		IIOMetadataNode node = new IIOMetadataNode(nodeName);
		rootNode.appendChild(node);
		
		return node;
	}
	
	/**
	 * Inserts a new node, replaces any already existing node with the same name
	 * 
	 * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
	 * @param node the node to insert
	 * 
	 * @return the node
	 */
	private static IIOMetadataNode insertNode(IIOMetadataNode rootNode, IIOMetadataNode node) {
		int nNodes = rootNode.getLength();
		for(int i = 0; i < nNodes; i++) {
			if(rootNode.item(i).getNodeName().compareToIgnoreCase(node.getNodeName()) == 0) {
				rootNode.removeChild(rootNode.item(i));
				
				break;
			}
		}
		
		rootNode.appendChild(node);
		
		return node;
	}
}