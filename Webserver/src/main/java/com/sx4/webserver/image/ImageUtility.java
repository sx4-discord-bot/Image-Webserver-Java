package com.sx4.webserver.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import com.sx4.webserver.gif.GifWriter;
import com.sx4.webserver.gif.MetadataUtility;

public class ImageUtility {
	
	public static BufferedImage fillImage(BufferedImage image, Color colour) {
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(colour);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		
		return image;
	}
	
	public static int getSetSizeText(Graphics2D graphics, int maxWidth, Font font, int fontSize, String text) {
		font = font.deriveFont(0, fontSize);
		while (graphics.getFontMetrics(font).stringWidth(text) > maxWidth) {
			fontSize -= 1;
			font = font.deriveFont(0, fontSize);
		}
		
		return fontSize;
	}
	
	public static Entry<String, ByteArrayOutputStream> updateEachFrame(URL url, Function<BufferedImage, BufferedImage> function) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		ImageInputStream stream = ImageIO.createImageInputStream(url.openStream());
		Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
		
		if(!readers.hasNext()) {
			throw new IIOException("No reader for the source is available");
		}
		
		ImageReader reader = readers.next();
		reader.setInput(stream);
		
		int frames = reader.getNumImages(true);
		if(frames > 1) {
			IIOMetadata metadata = reader.getImageMetadata(0);
			IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
			
			IIOMetadataNode descriptor = MetadataUtility.getNode(root, "ImageDescriptor");
			
			BufferedImage product = new BufferedImage(Integer.valueOf(descriptor.getAttribute("imageWidth")), Integer.valueOf(descriptor.getAttribute("imageHeight")), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = product.createGraphics();
			
			IIOMetadataNode graphicControl = MetadataUtility.getNode(root, "GraphicControlExtension");
			IIOMetadataNode applicationExtension = MetadataUtility.getNode(root, "ApplicationExtensions");
			
			GifWriter writer = new GifWriter(output, BufferedImage.TYPE_INT_ARGB, graphicControl, applicationExtension);
			
			for(int i = 0; i < frames; i++) {
				BufferedImage frame;
				try {
					frame = reader.read(i);
				}catch(Exception e) {
					System.err.println(String.format("Skipping frame %s in %s due to %s", (i + 1) + "/" + frames, url, e));
					
					continue;
				}
				
				IIOMetadata frameMetadata = reader.getImageMetadata(i);
				IIOMetadataNode frameRoot = (IIOMetadataNode) frameMetadata.getAsTree(frameMetadata.getNativeMetadataFormatName());
				
				IIOMetadataNode frameDescriptor = MetadataUtility.getNode(frameRoot, "ImageDescriptor");
				if(frameDescriptor != null) {
					int x = Integer.parseInt(frameDescriptor.getAttribute("imageLeftPosition"));
					int y = Integer.parseInt(frameDescriptor.getAttribute("imageTopPosition"));;
					
					graphics.drawImage(frame, x, y, null);
				}else{
					graphics.drawImage(frame, 0, 0, null);
				}
				
				writer.write(function.apply(deepCopy(product)));
			}
			
			writer.finish();
		}else{
			ImageIO.write(function.apply(reader.read(0)), "png", output);
		}
		
		return Map.entry(frames > 1 ? "gif" : "png", output);
	}
	
	public static int getRGBAValue(int value) {
		return Math.min(Math.max(value, 0), 255);
	}
	
	public static int asRGBA(int r, int g, int b, int a) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
	}
	
	public static String getNewLinedText(String text, int charsPerLine) {
		int times = (int) Math.ceil(text.length()/(double) charsPerLine);
		int n = 0, m = charsPerLine;
		String newText = "";
		for (int i = 0; i < times; i++) {
			if (n != 0) {
				while (text.charAt(n) != ' ' && text.length() != n) {
					if (n != 0) {
						n -= 1;
					} else {
						n = ((i + 1) * charsPerLine) - charsPerLine;
						break;
					}
				}
			}
			if (text.length() >= m) {
				while (text.charAt(m) != ' ' && text.length() != m) {
					if (m != 0) {
						m -= 1;
					} else {
						m = (i + 1) * charsPerLine;
						break;
					}
				}
			}
			newText += text.substring(n, Math.min(text.length(), m)).trim() + "\n";
			n += charsPerLine;
			m += charsPerLine;
		}
		
		return newText;
	}
	
	public static String getNewLinedWidthText(Graphics2D graphics, Font font, String text, int maxWidth) {
		String[] splitText = text.trim().split(" ");
		String newText = "";
		int width = 0, n = 0;
		for (String word : splitText) {
			word += " ";
			int m = word.length();
			int textWidth = graphics.getFontMetrics(font).stringWidth(word);
			if (textWidth > maxWidth) {
				while (true) {
					String cutWord = word.substring(n, m);
					int cutWordWidth = graphics.getFontMetrics(font).stringWidth(cutWord);
					if (cutWordWidth > maxWidth) {
						m -= 1;
					} else {
						newText += cutWord + "\n";
						if (m == word.length()) {
							break;
						} else {
							n = m;
							m = word.length();
						}
					}
				}
			} else {
				width += textWidth;
				if (width > maxWidth) {
					newText += "\n" + word;
					width = textWidth;
				} else {
					newText += word;
				}
			}
		} 
		
		return newText;
	}
	
	public static void drawText(Graphics2D graphics, String text, int x, int y) {
		drawText(graphics, text, x, y, 0);
	}

	public static void drawText(Graphics2D graphics, String text, int x, int y, int padding) {
		int lineHeight = graphics.getFontMetrics().getHeight();
		
		String[] lines = text.split("\n");
		for(int lineCount = 0; lineCount < lines.length; lineCount++) {
			graphics.drawString(lines[lineCount], x, y + lineCount * (lineHeight + padding));
		}
	}
	
	public static BufferedImage circlify(Image image) {
		if(image.getHeight(null) != image.getWidth(null)) {
			throw new IllegalArgumentException("Image width is not the same as the height");
		}
		
		int width = image.getWidth(null);
		
		BufferedImage circleBuffer = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D graphics = circleBuffer.createGraphics();
		
		graphics.setClip(new Ellipse2D.Float(0, 0, width, width));
		graphics.drawImage(image, 0, 0, width, width, null);
		
		return circleBuffer;
	}
	
	public static BufferedImage rotate(BufferedImage image, double degrees) {
		double angle = Math.toRadians(degrees);
		double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
		
		int width = image.getWidth(), height = image.getHeight();
		int newWidth = (int) Math.floor(width * cos + height *sin), newHeight = (int) Math.floor(height * cos + width * sin);
		
		BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D graphics = result.createGraphics();
		graphics.translate((newWidth - width) / 2, (newHeight - height) / 2);
		graphics.rotate(angle, width / 2, height / 2);
		graphics.drawRenderedImage(image, null);
		graphics.dispose();
		
		return result;
	}
	
	public static BufferedImage asBufferedImage(Image image) {
		BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = bufferedImage.createGraphics();
		graphics.drawImage(image, 0, 0, null);

		return bufferedImage;
	}
	
	public static BufferedImage deepCopy(BufferedImage image) {
		ColorModel colorModel = image.getColorModel();
		WritableRaster raster = image.copyData(null);
		
		boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
		
		return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
	}
	
	public static byte[] getImageBytes(BufferedImage image) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return outputStream.toByteArray();
	}
}