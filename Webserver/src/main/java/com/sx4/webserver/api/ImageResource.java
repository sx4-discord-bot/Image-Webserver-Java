package com.sx4.webserver.api;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import com.jhlabs.image.EdgeFilter;
import com.jhlabs.image.EmbossFilter;
import com.jhlabs.image.GaussianFilter;
import com.sx4.webserver.Fonts;
import com.sx4.webserver.gif.GifWriter;
import com.sx4.webserver.image.Body;
import com.sx4.webserver.image.CannyEdgeDetector;
import com.sx4.webserver.image.ImageUtility;
import org.json.JSONArray;
import org.json.JSONObject;

@Path("")
public class ImageResource {
	
	private static Random random = new Random();
	
	public static final String IMAGE_PATH = "resources/images/";
	
	private String getParameterType(Parameter parameter) {
		String typeName = parameter.getType().getName();
		
		String type;
		if (typeName.contains(".")) {
			String[] typeSplit = typeName.split("\\.");
			type = typeSplit[typeSplit.length - 1];
		} else {
			type = typeName;
		}
		
		return type;
	}
	
	@GET
	@Path("/endpoints")
	@Produces({"text/plain"})
	public Response getEndpoints() {
		StringBuilder stringBuilder = new StringBuilder();
		
		int maxLength = 0;
		List<Integer> maxLengthParameters = new ArrayList<>();
		for (Method method : ImageResource.class.getDeclaredMethods()) {	
		    if (method.isAnnotationPresent(Path.class)) {
		    	maxLength = Math.max(maxLength, ("/api" + method.getAnnotation(Path.class).value() + "/").length());
		    	
		    	Parameter[] parameters = method.getParameters();
		    	for (int i = 0; i < parameters.length; i++) {
		    		Parameter parameter = parameters[i];
		    		if (parameter.isAnnotationPresent(QueryParam.class)) {
			    		String query = " " + this.getParameterType(parameter) + " " + parameter.getAnnotation(QueryParam.class).value();
			    		
			    		if (maxLengthParameters.size() - 1 <= i) {
			    			maxLengthParameters.add(query.length());
			    		} else {
			    			maxLengthParameters.remove(i);
			    			maxLengthParameters.add(i, Math.max(query.length(), maxLengthParameters.get(i)));
			    		}
		    		}
		    	}
		    }
		}
		
		for (Method method : ImageResource.class.getDeclaredMethods()) {	
			if (method.isAnnotationPresent(GET.class)) {
		    	stringBuilder.append("GET     ");
		    } else if (method.isAnnotationPresent(POST.class)) {
		    	stringBuilder.append("POST    ");
		    }
			
		    if (method.isAnnotationPresent(Path.class)) {
		    	stringBuilder.append(String.format("%-" + (maxLength + 5) + "s", "/api" + method.getAnnotation(Path.class).value() + "/"));
		    	
		    	Parameter[] newParameters = method.getParameters();
				for (int i = 0; i < newParameters.length; i++) {
					Parameter parameter = newParameters[i];
					if (parameter.isAnnotationPresent(QueryParam.class)) {
			    		stringBuilder.append(String.format("%-" + (maxLengthParameters.get(i) + 5) + "s", " " + this.getParameterType(parameter) + " " + parameter.getAnnotation(QueryParam.class).value()));
					}
				}
				
				stringBuilder.append("\n");
		    }
		}
		
		String[] splitText = stringBuilder.toString().split("\n");
		Arrays.sort(splitText, (a, b) -> Integer.compare(b.trim().length(), a.trim().length()));
		
		return Response.ok("All endpoints are as listed:\n\n" + String.join("\n", splitText)).build();
	}
	
	@GET
	@Path("/resize")
	@Produces({"image/png", "text/plain"})
	public Response getResizedImage(@QueryParam("image") String imageUrl, @QueryParam("height") Integer height, @QueryParam("width") Integer width) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(imageUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("That is not a valid url").header("Content-Type", "text/plain").build();
		}
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				return ImageUtility.asBufferedImage(frame.getScaledInstance(width == null ? frame.getWidth() : width, height == null ? frame.getHeight() : height, Image.SCALE_DEFAULT));
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/crop")
	@Produces({"image/png", "text/plain"})
	public void getCroppedImage(@Suspended final AsyncResponse asyncResponse, @QueryParam("image") String imageUrl, @QueryParam("height") Integer height, @QueryParam("width") Integer width) throws Exception {
		if (width < 1 || height < 1) {
			asyncResponse.resume(Response.status(400).entity("Height and width both have to be positive").header("Content-Type", "text/plain").build());
			return;
		}
		
		URL url;
		try {
			url = new URL(URLDecoder.decode(imageUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			asyncResponse.resume(Response.status(400).entity("That is not a valid url").header("Content-Type", "text/plain").build());
			return;
		}
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				if (width > frame.getWidth() || height > frame.getHeight()) {
					asyncResponse.resume(Response.status(400).entity("You cannot crop an image bigger than its original size").header("Content-Type", "text/plain").build());
				}
				
				return frame.getSubimage((frame.getWidth() / 2) - ((width == null ? frame.getWidth() : width) / 2), (frame.getHeight() / 2) - ((height == null ? frame.getHeight() : height) / 2), width == null ? frame.getWidth() : width, height == null ? frame.getHeight() : height);
			});
			
			asyncResponse.resume(Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build());	
		} catch (IIOException e) {
			asyncResponse.resume(Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build());
		}
	}
	
	@GET
	@Path("/hot")
	@Produces({"image/png", "text/plain"})
	public Response getHotImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage background = new BufferedImage(419, 493, BufferedImage.TYPE_INT_ARGB);
		BufferedImage image = ImageUtility.asBufferedImage(ImageIO.read(new File(IMAGE_PATH + "thats-hot-meme.png")).getScaledInstance(419, 493, Image.SCALE_DEFAULT));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(400, 300, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = background.createGraphics();
				graphics.drawImage(frame, 8, 213, null);
				graphics.drawImage(image, 0, 0, null);
				
				return background;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/flag")
	@Produces({"image/png", "text/plain"})
	public Response getFlagImage(@QueryParam("image") String query, @QueryParam("flag") String flagQuery) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage flag;
		try {
			flag = ImageUtility.asBufferedImage(ImageIO.read(new URL("http://www.geonames.org/flags/x/" + flagQuery + ".gif")).getScaledInstance(200, 200, Image.SCALE_DEFAULT));
		} catch (Exception e) {
			return Response.status(400).entity("Flag initial is invalid").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageUtility.asBufferedImage(new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB).getScaledInstance(200, 200, Image.SCALE_DEFAULT));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = image.createGraphics();
				graphics.drawImage(frame, 0, 0, null);
				Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35F);
				graphics.setComposite(composite);
				graphics.drawImage(flag, 0, 0, null);
				
				return image;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/trash")
	@Produces({"image/png", "text/plain"})
	public Response getTrashImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage avatar;
		try {
			avatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The url given is not an image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "trash-meme.jpg"));
		Image resizedAvatar = avatar.getScaledInstance(385, 384, Image.SCALE_DEFAULT);
		
		GaussianFilter filter = new GaussianFilter(20);
		BufferedImage blurredAvatar = filter.filter(ImageUtility.asBufferedImage(resizedAvatar), null);
		
		Graphics graphics = image.getGraphics();
		graphics.drawImage(blurredAvatar, 384, 0, null);
		
		return Response.ok(ImageUtility.getImageBytes(image)).build();	
	}
	
	@GET
	@Path("/www")
	@Produces({"image/png", "text/plain"})
	public Response getWhoWouldWinImage(@QueryParam("firstImage") String firstQuery, @QueryParam("secondImage") String secondQuery) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(firstQuery, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("First image/user is invalid").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage firstAvatar;
		try {
			firstAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The first url given is not an image").header("Content-Type", "text/plain").build();
		}
		
		try {
			url = new URL(URLDecoder.decode(secondQuery, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Second image/user is invalid").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage secondAvatar;
		try {
			secondAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The second url given is not an image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "whowouldwin.png"));
		Image firstResizedAvatar = firstAvatar.getScaledInstance(400, 400, Image.SCALE_DEFAULT);
		Image secondResizedAvatar = secondAvatar.getScaledInstance(400, 400, Image.SCALE_DEFAULT);
		
		Graphics graphics = image.getGraphics();
		graphics.drawImage(firstResizedAvatar, 30, 180, null);
		graphics.drawImage(secondResizedAvatar, 510, 180, null);
		
		return Response.ok(ImageUtility.getImageBytes(image)).build();	
	}
	
	@GET
	@Path("/fear")
	@Produces({"image/png", "text/plain"})
	public Response getFearImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "fear-meme.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(251, 251, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = image.createGraphics();
				graphics.drawImage(frame, 260, 517, null);
				
				return image;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/emboss")
	@Produces({"image/png", "text/plain"})
	public Response getEmbossImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		EmbossFilter filter = new EmbossFilter();
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				BufferedImage embossAvatar = filter.filter(frame, null);
				
				return embossAvatar;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/canny")
	@Produces({"image/png", "text/plain"})
	public Response getCannyImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		CannyEdgeDetector canny = new CannyEdgeDetector();
		canny.setLowThreshold(0.5F);
		canny.setHighThreshold(1F);
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				BufferedImage frameBackground = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
				
				Graphics2D graphics = frameBackground.createGraphics();
				graphics.drawImage(frame, 0, 0, null);
				
				canny.setSourceImage(frameBackground);
				canny.process();
				
				return canny.getEdgesImage();
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/edge")
	@Produces({"image/png", "text/plain"})
	public Response getEdgeImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		EdgeFilter filter = new EdgeFilter();
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				BufferedImage image = filter.filter(frame, null);
				
				return image;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/invert")
	@Produces({"image/png", "text/plain"})
	public Response getInvertedImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				for (int height = 0; height < frame.getHeight(); height++) {
					for (int width = 0; width < frame.getWidth(); width++) {
						Color oldColour = new Color(frame.getRGB(width, height));
						frame.setRGB(width, height, new Color(255 - oldColour.getRed(), 255 - oldColour.getGreen(), 255 - oldColour.getBlue()).hashCode());
					}
				}
				
				return frame;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/ship")
	@Produces({"image/png", "text/plain"})
	public Response getShipImage(@QueryParam("firstImage") String firstQuery, @QueryParam("secondImage") String secondQuery) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(firstQuery, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("First user is invalid").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage firstAvatar;
		try {
			firstAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The first url given is not an image").header("Content-Type", "text/plain").build();
		}
		
		try {
			url = new URL(URLDecoder.decode(secondQuery, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Second user is invalid").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage secondAvatar;
		try {
			secondAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The second url given is not an image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = new BufferedImage(930, 290, BufferedImage.TYPE_INT_ARGB);
		BufferedImage heart = ImageIO.read(new File(IMAGE_PATH + "heart.png"));
		Image avatarOutline = ImageUtility.circlify(ImageUtility.fillImage(new BufferedImage(290, 290, BufferedImage.TYPE_INT_ARGB), Color.WHITE));
		Image firstResizedAvatar = ImageUtility.circlify(firstAvatar.getScaledInstance(280, 280, Image.SCALE_DEFAULT));
		Image secondResizedAvatar = ImageUtility.circlify(secondAvatar.getScaledInstance(280, 280, Image.SCALE_DEFAULT));
		
		Graphics graphics = image.getGraphics();
		graphics.drawImage(avatarOutline, 0, 0, null);
		graphics.drawImage(avatarOutline, 640, 0, null);
		graphics.drawImage(firstResizedAvatar, 5, 5, null);
		graphics.drawImage(heart, 305, 0, null);
		graphics.drawImage(secondResizedAvatar, 645, 5, null);
		
		return Response.ok(ImageUtility.getImageBytes(image)).build();	
	}
	
	@GET
	@Path("/vr")
	@Produces({"image/png", "text/plain"})
	public Response getVrImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
			
		BufferedImage background = new BufferedImage(493, 511, BufferedImage.TYPE_INT_ARGB);
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "vr.png"));
		Image resizedImage = image.getScaledInstance(493, 511, Image.SCALE_DEFAULT);
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				BufferedImage resizedAvatar = ImageUtility.asBufferedImage(frame.getScaledInstance(225, 150, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = background.createGraphics();
				graphics.drawImage(resizedAvatar, 15, 310, null);
				graphics.drawImage(resizedImage, 0, 0, null);
				
				return background;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/shit")
	@Produces({"image/png", "text/plain"})
	public Response getShitImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage background = new BufferedImage(763, 1080, BufferedImage.TYPE_INT_ARGB);
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "shit-meme.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(192, 192, Image.SCALE_DEFAULT));
	
				AffineTransform transform = new AffineTransform();
				transform.rotate(Math.toRadians(-50), frame.getWidth()/2, frame.getHeight()/2);
				AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
				frame = op.filter(frame, null);
				
				Graphics2D graphics = background.createGraphics();
				graphics.drawImage(frame, 240, 700, null);
				graphics.drawImage(image, 0, 0, null);
				
				return background;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/gay")
	@Produces({"image/png", "text/plain"})
	public Response getGayImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "gay.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				frame = ImageUtility.asBufferedImage(frame);
				BufferedImage resizedImage = ImageUtility.asBufferedImage(image.getScaledInstance(frame.getWidth(), frame.getHeight(), Image.SCALE_DEFAULT));
				
				Graphics2D graphics = frame.createGraphics();
				graphics.drawImage(resizedImage, 0, 0, null);
				
				return frame;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/beautiful")
	@Produces({"image/png", "text/plain"})
	public Response getBeautifulImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image").header("Content-Type", "text/plain").build();
		}

		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "beautiful.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {	
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(90, 104, Image.SCALE_DEFAULT));
				
				frame = ImageUtility.rotate(frame, -1);
				
				Graphics2D graphics = image.createGraphics();
				graphics.drawImage(frame, 253, 25, null);
				graphics.drawImage(frame, 256, 222, null);
				
				return image;	
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	public enum MentionType {
		ROLE("@&"),
		USER("@!"),
		CHANNEL("#"),
		EMOTE(":", "a:"),
		UNKNOWN;
		
		private String[] prefixes;
		
		private MentionType(String... prefixes) {
			this.prefixes = prefixes;
		}
		
		private String[] getPrefixes() {
			return this.prefixes;
		}
		
		public static MentionType getByPrefix(String prefix) {
			for (MentionType mentionType : MentionType.values()) {
				for (String mentionPrefix : mentionType.getPrefixes()) {
					if (mentionPrefix.equals(prefix)) {
						return mentionType;
					}
				}
			}
			
			return MentionType.UNKNOWN;
		}
	}
	
	@POST
	@Path("/discord")
	@Produces({"image/png", "text/plain", "image/gif"})
	public Response getDiscordImage(Body body) throws Exception {
		String text = body.getString("text");
		String userName = body.getString("userName");
		String colour = body.getString("colour");
		String avatarUrl = body.getString("avatarUrl");
		
		Map<String, Map<String, String>> users = body.get("users", Map.of());
		Map<String, Map<String, String>> emotes = body.get("emotes", Map.of());
		Map<String, Map<String, String>> channels = body.get("channels", Map.of());
		Map<String, Map<String, String>> roles = body.get("roles", Map.of());
		
		boolean darkTheme = body.get("darkTheme", true);
		boolean bot = body.getBoolean("bot");
		
		URL url;
		try {
			url = new URL(URLDecoder.decode(avatarUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user").header("Content-Type", "text/plain").build();
		}
		
		URL emoteUrl;
		try {
			emoteUrl = new URL(URLDecoder.decode("https://cdn.discordapp.com/emojis/441255212582174731.png", StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid emote").header("Content-Type", "text/plain").build();
		}
		
		Image botImage;
		try {
			botImage = ImageIO.read(emoteUrl).getScaledInstance(60, 60, Image.SCALE_DEFAULT);
		} catch (Exception e) {
			return Response.status(400).entity("The bot emote url is not an image").header("Content-Type", "text/plain").build();
		}
		
		int breaks = text.trim().split("\n").length - 1; 
		
		int times = (int) Math.ceil(text.length()/50D);
		
		int height = (breaks * 36) + (times * 36);
		int length = bot ? 66 : 0;
		
		Font mainText = Fonts.WHITNEY_BOOK.deriveFont(0, 34);
		Font nameText = Fonts.WHITNEY_MEDIUM.deriveFont(0, 40);
		Font timeText = Fonts.WHITNEY_LIGHT.deriveFont(0, 24);
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {
				frame = ImageUtility.circlify(ImageUtility.asBufferedImage(frame.getScaledInstance(100, 100, BufferedImage.TYPE_INT_ARGB)));
				
				BufferedImage image = new BufferedImage(1000, 115 + height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				
				RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				graphics.setRenderingHints(hints);
				
				int nameWidth = graphics.getFontMetrics(nameText).stringWidth(userName);
				int nameHeight = 40;
				
				graphics.setColor(darkTheme ? new Color(54, 57, 63) : Color.WHITE);
				graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
				graphics.drawImage(frame, 20, 10, null);
				if (bot) {
					graphics.drawImage(botImage, 170 + nameWidth, 2, null);
				}
				
				graphics.setColor(Color.decode("#" + colour));
				graphics.setFont(nameText);
				graphics.drawString(userName, 160, 6 + nameHeight);
				graphics.setColor(new Color(122, 125, 130));
				graphics.setFont(timeText);
				graphics.drawString("Today at " + LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm")), 170 + nameWidth + length, (nameHeight/2) - 2 + 24);
				
				Color mentionBox = new Color(114, 137, 218, 26);
				Color mentionText = new Color(114, 137, 218);
				Color textColour = !darkTheme ? new Color(116, 127, 141) : Color.WHITE;
				graphics.setColor(textColour);
				graphics.setFont(mainText);
				
				int textHeight = nameHeight + 54, textWidth = 160, fontHeight = graphics.getFontMetrics().getHeight();
				
				String[] splitText = text.trim().split(" ");
				for (int index = 0; index < splitText.length; index++) {
					String word = splitText[index] + " ";
					if (word.contains("<") && word.contains(">")) {
						for (int i = 0; i < word.length(); i++) {
							char character = word.charAt(i);
							if (character == '<' && (i == 0 || word.charAt(i - 1) != '\\')) {
								int moreThanIndex = word.indexOf('>', i + 1);
								
								if (moreThanIndex != -1 && word.charAt(moreThanIndex - 1) != '\\') {
									StringBuilder mentionPrefix = new StringBuilder();
									
									int prefixIndex = i;
									if (word.charAt(prefixIndex + 1) == '@' && word.charAt(prefixIndex + 2) != '!' && word.charAt(prefixIndex + 2) != '&') {
										mentionPrefix.append("@");
									} else {
							        	while (MentionType.getByPrefix(mentionPrefix.toString()) == MentionType.UNKNOWN && ++prefixIndex != moreThanIndex) {
							        		mentionPrefix.append(word.charAt(prefixIndex));
							        	}
									}
						        	
						        	if (++prefixIndex != moreThanIndex) {
						        		String mentionString, id;
						        		int mentionStringWidth;
										switch (MentionType.getByPrefix(mentionPrefix.toString())) {
							        		case USER:
							        			id = word.substring(prefixIndex, moreThanIndex);
							        			Map<String, String> user = users.get(id);
							        			
							        			mentionString = user == null ? "<" + mentionPrefix.toString() + id + ">" : "@" + user.get("name");
							        			
							        			mentionStringWidth = graphics.getFontMetrics().stringWidth(mentionString);
							        			if (textWidth + mentionStringWidth > 975) {
													textHeight += fontHeight;
													textWidth = 160;
												} 
							        			
							        			graphics.setColor(mentionBox);
							        			graphics.fillRect(textWidth - 2, textHeight - fontHeight + 11, mentionStringWidth + 5, fontHeight - 5);
							        			graphics.setColor(mentionText);
							        			
								        		graphics.drawString(mentionString, textWidth, textHeight);
								        		textWidth += mentionStringWidth + 7;
							        			
							        			graphics.setColor(textColour);
						        				
						        				i = moreThanIndex;
							        			
							        			continue;
							        		case ROLE:
							        			id = word.substring(prefixIndex, moreThanIndex);
							        			Map<String, String> role = roles.get(id);
							        			
							        			mentionString = role == null ? "<" + mentionPrefix.toString() + id + ">" : "@" + role.get("name");
							        			String roleColour = role == null ? null : role.get("colour");
							        			
							        			mentionStringWidth = graphics.getFontMetrics().stringWidth(mentionString);
							        			if (textWidth + mentionStringWidth > 975) {
													textHeight += fontHeight;
													textWidth = 160;
												} 
							        			
							        			Color roleMentionColour = null;
							        			if (roleColour != null) {
							        				roleMentionColour = Color.decode("#" + roleColour);
							        			}
							        			
							        			graphics.setColor(roleColour == null ? mentionBox : new Color(roleMentionColour.getRed(), roleMentionColour.getGreen(), roleMentionColour.getBlue(), 26));
							        			graphics.fillRect(textWidth - 2, textHeight - fontHeight + 11, mentionStringWidth + 5, fontHeight - 5);
							        			graphics.setColor(roleColour == null ? mentionText : Color.decode("#" + roleColour));
							        			
							        			graphics.drawString(mentionString, textWidth, textHeight);
							        			textWidth += mentionStringWidth + 7;
							        			
							        			graphics.setColor(textColour);
							        			
							        			i = moreThanIndex;
							        			
							        			continue;
							        		case CHANNEL:
							        			id = word.substring(prefixIndex, moreThanIndex);
							        			Map<String, String> channel = channels.get(id);
							        			
							        			mentionString = channel == null ? "#deleted-channel" : "#" + channel.get("name");
							        			
							        			mentionStringWidth = graphics.getFontMetrics().stringWidth(mentionString);
							        			if (textWidth + mentionStringWidth > 975) {
													textHeight += fontHeight;
													textWidth = 160;
												} 
							        			
							        			if (channel != null) {
								        			graphics.setColor(mentionBox);
								        			graphics.fillRect(textWidth - 2, textHeight - fontHeight + 11, mentionStringWidth + 5, fontHeight - 5);
								        			graphics.setColor(mentionText);
							        			}
							        			
							        			graphics.drawString(mentionString, textWidth, textHeight);
							        			textWidth += mentionStringWidth + 7;
							        			
							        			if (channel != null) {
							        				graphics.setColor(textColour);
							        			}
							        			
							        			i = moreThanIndex;
							        			
							        			continue;
							        		case EMOTE:
							        			int nextColonIndex = i + 1 + mentionPrefix.length();
							        			
							        			while (word.charAt(++nextColonIndex) != ':' && nextColonIndex != moreThanIndex);
							        			
							        			if (nextColonIndex != moreThanIndex) {
							        				id = word.substring(nextColonIndex + 1, moreThanIndex);
								        			Map<String, String> emote = emotes.get(id);
								        			if (emote != null) {
								        				String mentionEmoteUrl = emote.get("url");
								        				
								        				try {
								        					Image emoteImage = ImageIO.read(new URL(mentionEmoteUrl)).getScaledInstance(fontHeight, fontHeight, Image.SCALE_DEFAULT);
								        					
								        					if (textWidth + fontHeight > 975) {
																textHeight += fontHeight;
																textWidth = 160;
															} 
								        					
								        					graphics.drawImage(emoteImage, textWidth, textHeight - 31, null);
								        					textWidth += fontHeight;
										        			
										        			i = moreThanIndex;
								        					
								        					continue;
								        				} catch (Exception e) {}
								        			}
							        			}
							        		default:
							        			break;
										}
						        	}
								}
							}
							
							if (character != '\\') {
								int charWidth = graphics.getFontMetrics().charWidth(character);
								if (textWidth + charWidth > 975) {
									textHeight += fontHeight;
									textWidth = 160;
								}
								
								graphics.drawString(String.valueOf(character), textWidth, textHeight);
								
								textWidth += charWidth;
							}
						}
					} else {
						int wordWidth = graphics.getFontMetrics().stringWidth(word);
						if (wordWidth > 975) {
							int end = word.length();
							int start = 0;
							while (true) {
								String cutWord = word.substring(start, end);
								int cutWordWidth = graphics.getFontMetrics().stringWidth(cutWord);
								if ((start == 0 && cutWordWidth > 975 - textWidth) || cutWordWidth > 820) {
									end -= 1;
								} else {
									graphics.drawString(cutWord, textWidth, textHeight);
									textWidth = 160;
									textHeight += fontHeight;
									
									if (end == word.length()) {
										break;
									} else {
										start = end;
										end = word.length();
									}
								}
							}
						} else {
							if (textWidth + wordWidth > 975) {
								textHeight += fontHeight;
								textWidth = 160;
							}
							
							graphics.drawString(word, textWidth, textHeight);
							
							textWidth += wordWidth;
						}
					}
				}
				
				return image.getSubimage(0, 0, 1000, textHeight + 25);
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/trump")
	@Produces({"image/png", "text/plain"})
	public Response getTrumpImage(@QueryParam("text") String query) throws Exception {	
		String text = ImageUtility.getNewLinedText(query, 70);
		
		Font textFont = new Font("Arial", 0, 25);
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "trumptweet-meme.png"));
		
		Graphics2D graphics = image.createGraphics();
		
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHints(hints);
		
		graphics.setColor(Color.BLACK);
		graphics.setFont(textFont);
		ImageUtility.drawText(graphics, text, 60, 150);
		
		return Response.ok(ImageUtility.getImageBytes(image)).build();
	}
	
	@POST
	@Path("/tweet")
	@Consumes("application/json")
	@Produces({"image/png", "text/plain"})
	public Response getTweetImage(Body body) throws Exception {
		String displayName = body.getString("displayName");
		String tagName = body.getString("name");
		String avatarUrl = body.getString("avatarUrl");
		List<String> likeAvatarUrls = body.getList("urls", String.class);
		int likes = body.getInteger("likes");
		int retweets = body.getInteger("retweets");
		String text = body.getString("text");
		
		URL url;
		try {
			url = new URL(avatarUrl);
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user").header("Content-Type", "text/plain").build();
		}
		
		Image avatar;
		try {
			avatar = ImageIO.read(url).getScaledInstance(72, 72, Image.SCALE_DEFAULT);
		} catch (Exception e) {
			return Response.status(400).entity("The url given is not an image").header("Content-Type", "text/plain").build();
		}

		List<BufferedImage> likeAvatars = new ArrayList<BufferedImage>();
		for (String av : likeAvatarUrls) {
			try {
				url = new URL(av);
			} catch (Exception e) {
				return Response.status(400).entity("One of the random avatar urls is invalid").header("Content-Type", "text/plain").build();
			}
				
			try {
				likeAvatars.add(ImageUtility.circlify(ImageIO.read(url).getScaledInstance(36, 36, Image.SCALE_DEFAULT)));
			} catch (Exception e) {
				likeAvatars.add(ImageUtility.circlify(ImageIO.read(new URL("https://cdn.discordapp.com/embed/avatars/" + random.nextInt(5) + ".png")).getScaledInstance(36, 36, Image.SCALE_DEFAULT)));
			}
		}
		
		LocalDateTime time = LocalDateTime.now(ZoneOffset.UTC);
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "tweet.png"));
		
		Font nameFont = Fonts.GOTHAM_BLACK.deriveFont(0, 25);
		Font tagFont = Fonts.GOTHAM_BOOK.deriveFont(0, 20);
		Font likesFont = Fonts.GOTHAM_BOLD.deriveFont(Font.BOLD, 21);
		Font textFont = Fonts.SEGOE_UI.deriveFont(0, 25);
		
		Graphics2D graphics = image.createGraphics();
		
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHints(hints);
		
		graphics.setFont(textFont);

		String[] splitNewLineText = ImageUtility.getNewLinedWidthText(graphics, textFont, text, 833).split("\n");
		int width = 60, height = 155;
		for (String newLine : splitNewLineText) {
			String[] splitText = newLine.split(" ");
			for (String word : splitText) {
				if (word.startsWith("#") || word.startsWith("@")) {
					graphics.setColor(Color.decode("#0084b4"));
					graphics.drawString(word + " ", width, height);
				} else {
					graphics.setColor(Color.BLACK);
					graphics.drawString(word + " ", width, height);
				}
				
				width += graphics.getFontMetrics(textFont).stringWidth(word + " ");
			}
			
			height += 30;
			width = 60;
		}
		
		graphics.drawImage(ImageUtility.circlify(avatar), 60, 44, null);
		
		graphics.setColor(Color.BLACK);
		graphics.setFont(nameFont);
		graphics.drawString(displayName, 149, 72);
		graphics.setColor(Color.GRAY);
		graphics.setFont(tagFont);
		graphics.drawString("@" + tagName, 149, 102);
		graphics.setColor(Color.BLACK);
		graphics.setFont(likesFont);
		String retweetsText = NumberFormat.getNumberInstance(Locale.UK).format(retweets);
		graphics.drawString(retweetsText, 59, 342);
		int retweetsWidth = graphics.getFontMetrics(likesFont).stringWidth(retweetsText);
		graphics.setColor(Color.GRAY);
		graphics.setFont(tagFont);
		graphics.drawString("Retweets", 64 + retweetsWidth, 342);
		int retweetTextWidth = graphics.getFontMetrics(tagFont).stringWidth("Retweets");
		String likesText = NumberFormat.getNumberInstance(Locale.UK).format(likes);
		graphics.setColor(Color.BLACK);
		graphics.setFont(likesFont);
		graphics.drawString(likesText, 77 + retweetsWidth + retweetTextWidth, 342);
		int likesWidth = graphics.getFontMetrics(likesFont).stringWidth(likesText);
		graphics.setColor(Color.GRAY);
		graphics.setFont(tagFont);
		graphics.drawString("Likes", 82 + retweetsWidth + retweetTextWidth + likesWidth, 342);
		graphics.drawString(time.format(DateTimeFormatter.ofPattern("h:mm a")).toUpperCase() + time.format(DateTimeFormatter.ofPattern(" - dd LLL uuuu")), 60, 285);
		
		int additional = 0;
		for (BufferedImage likeAvatar : likeAvatars) {
			graphics.drawImage(likeAvatar, 398 + additional, 317, null);
			additional += 44;
		}
		
		return Response.ok(ImageUtility.getImageBytes(image)).build();
	}
	
	@GET
	@Path("/christmas")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getChristmasImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image url").header("Content-Type", "text/plain").build();
		}
		
		int width = 256;
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {		
				double widthPercent = width/(double) frame.getWidth();
				int height = (int) (frame.getHeight() * widthPercent);
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(width, height, Image.SCALE_DEFAULT));
				
				BufferedImage white = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				
				for (int w = 0; w < frame.getWidth(); w++) {
					for (int h = 0; h < frame.getHeight(); h++) {
						int rgb = frame.getRGB(w, h);
						int r = (rgb >> 16) & 0xFF;
						int g = (rgb >> 8) & 0xFF;
						int b = (rgb >> 0) & 0xFF;
						double o = Math.sqrt(Math.pow(r, 2)*0.299 + Math.pow(g, 2)*0.587 + Math.pow(b, 2)*0.114);
						o *= ((o - 102) / 128);
						o = 255 - o;
						frame.setRGB(w, h, ImageUtility.asRGBA(255, 0, 0, ImageUtility.getRGBAValue((int) o)));
					}
				}
				
				Graphics2D graphics = white.createGraphics();
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, white.getWidth(), white.getHeight());
				graphics.drawImage(frame, 0, 0, null);
				
				return white;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/halloween")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getHalloweenImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image url").header("Content-Type", "text/plain").build();
		}
		
		int width = 256;
		
		try {
			Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(url, (frame) -> {
				double widthPercent = width/(double) frame.getWidth();
				int height = (int) (frame.getHeight() * widthPercent);
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(width, height, Image.SCALE_DEFAULT));
				
				for (int w = 0; w < frame.getWidth(); w++) {
					for (int h = 0; h < frame.getHeight(); h++) {
						int rgb = frame.getRGB(w, h);
						int r = (rgb >> 16) & 0xFF;
						int g = (rgb >> 8) & 0xFF;
						int b = (rgb >> 0) & 0xFF;
						int a = (rgb >> 24) & 0xFF;
						double o = Math.sqrt(Math.pow(r, 2)*0.299 + Math.pow(g, 2)*0.587 + Math.pow(b, 2)*0.114);
						o *= ((o - 102) / 128);
						frame.setRGB(w, h, ImageUtility.asRGBA(ImageUtility.getRGBAValue((int) o), ImageUtility.getRGBAValue((int) ((o - 10) / 2)), 0, a));
					}
				}
				
				return frame;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image").header("Content-Type", "text/plain").build();
		}
	}
	
	private final Map<Boolean, Map<String, byte[]>> welcomerCache = new HashMap<>();
	
	private byte[] getWelcomerCache(boolean gif, String url) {
		if (this.welcomerCache.containsKey(gif)) {
			Map<String, byte[]> cache = this.welcomerCache.get(gif);
			return cache.get(url);
		}
		
		return null;
	}
	
	private void putWelcomerCache(boolean gif, String url, byte[] bytes) {
		this.welcomerCache.compute(gif, (key, value) -> {
			if (value == null) {
				Map<String, byte[]> newCache = new HashMap<>();

				newCache.put(url, bytes);
				
				return newCache;
			} else {
				value.put(url, bytes);
				
				return value;
			}
		});
	}
	
	@GET
	@Path("/welcomer")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getWelcomerImage(@QueryParam("background") String background, @QueryParam("userAvatar") String userAvatar, @QueryParam("userName") String userFullName, @QueryParam("gif") boolean gif) throws Exception {
		Map<String, byte[]> cache = this.welcomerCache.containsKey(gif) ? this.welcomerCache.get(gif) : new HashMap<>();
		boolean cached = false;
		
		URL backgroundUrl = null;
		if (background != null) {
			background = URLDecoder.decode(background, StandardCharsets.UTF_8);
			cached = cache.containsKey(background);
			
			try {
				backgroundUrl = new URL(background);
			} catch (Exception e) {
				return Response.status(400).entity(e.getMessage()).build();
			}
		}
		
		int heightWelcomeText, avatarX, avatarY, avatarZ, totalHeight;
		if (backgroundUrl != null) {
			heightWelcomeText = 238;
			avatarX = 220;
			avatarY = 205;
			avatarZ = 0;
			totalHeight = 720;
		} else {
			heightWelcomeText = 35;
			avatarX = 18;
			avatarY = 3;
			avatarZ = 3;
			totalHeight = 315;
		}
		
		URL userAvatarUrl;
		try {
			userAvatarUrl = new URL(URLDecoder.decode(userAvatar, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).build();
		}
		
		String[] userSplit = userFullName.split("#");
		String userName = userSplit[0];
		String userDiscrim = "#" + userSplit[1];
		
		Font discrimFont = Fonts.UNI_SANS.deriveFont(0, 50);
		
		int nameFontHeight = 100;
		int discrimFontHeight = 50;
		int width = 1420, height = 280;
		
		BufferedImage avatar;
		try {
			avatar = ImageUtility.circlify(ImageUtility.asBufferedImage(ImageIO.read(userAvatarUrl).getScaledInstance(300, 300, BufferedImage.TYPE_INT_ARGB)));
		} catch (Exception e) {
			return Response.status(400).build();
		}
		
		BufferedImage textHolder, avatarOutline;
		if (backgroundUrl == null || !cached) {
			textHolder = new BufferedImage(1280, 280, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gTextHolder = textHolder.createGraphics();
			gTextHolder.setColor(new Color(0, 0, 0, 200));
			gTextHolder.fillRect(0, 0, textHolder.getWidth(), textHolder.getHeight());
			
			BufferedImage avatarOutlineRectangle = new BufferedImage(310, 310, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = avatarOutlineRectangle.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, avatarOutlineRectangle.getWidth(), avatarOutlineRectangle.getHeight());
			
			avatarOutline = ImageUtility.circlify(avatarOutlineRectangle);
		} else {
			textHolder = null;
			avatarOutline = null;
		}
		
		if (backgroundUrl == null) {
			BufferedImage backgroundImage  = new BufferedImage(1280, totalHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphicsBackground = backgroundImage.createGraphics();
			
			int fontSize = ImageUtility.getSetSizeText(graphicsBackground, 1025 - graphicsBackground.getFontMetrics(discrimFont).stringWidth(userDiscrim) * 2, Fonts.UNI_SANS, 100, userName);
			Font nameFont = Fonts.UNI_SANS.deriveFont(0, fontSize);
			
			RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphicsBackground.setRenderingHints(hints);
			
			int nameFontWidth = graphicsBackground.getFontMetrics(nameFont).stringWidth(userName);
			int welcomeFontWidth = graphicsBackground.getFontMetrics(discrimFont).stringWidth("Welcome");
			
			graphicsBackground.drawImage(textHolder, 175 + avatarZ, avatarX, null);
		
			graphicsBackground.drawImage(avatarOutline, avatarZ, avatarY, null);
			graphicsBackground.drawImage(ImageUtility.circlify(avatar), avatarZ + 5, avatarY + 5, null);
			graphicsBackground.setFont(discrimFont);
			graphicsBackground.setColor(Color.WHITE);
			graphicsBackground.drawString("Welcome", (width-welcomeFontWidth)/2, heightWelcomeText + 40);
			graphicsBackground.setFont(nameFont);
			graphicsBackground.drawString(userName, (width-nameFontWidth)/2, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - 20));
			graphicsBackground.setFont(discrimFont);
			graphicsBackground.setColor(new Color(153, 170, 183));
			graphicsBackground.drawString(userDiscrim, (width-nameFontWidth)/2 + nameFontWidth, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - discrimFontHeight) + 30);
			
			return Response.ok(ImageUtility.getImageBytes(backgroundImage)).type("image/png").build();
		} else if (!cached) {
			if (!gif) {
				BufferedImage frame = ImageIO.read(backgroundUrl);
				
				frame = ImageUtility.asBufferedImage(frame.getScaledInstance(1280, totalHeight, Image.SCALE_DEFAULT));
				
				Graphics2D graphicsBackground = frame.createGraphics();
				
				RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				graphicsBackground.setRenderingHints(hints);
				
				int welcomeFontWidth = graphicsBackground.getFontMetrics(discrimFont).stringWidth("Welcome");
				
				graphicsBackground.drawImage(textHolder, 175 + avatarZ, avatarX, null);
			
				graphicsBackground.drawImage(avatarOutline, avatarZ, avatarY, null);
				graphicsBackground.setFont(discrimFont);
				graphicsBackground.setColor(Color.WHITE);
				graphicsBackground.drawString("Welcome", (width-welcomeFontWidth)/2, heightWelcomeText + 40);
				
				this.putWelcomerCache(gif, background, ImageUtility.getImageBytes(frame));
			} else {
				Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(backgroundUrl, (frame) -> {
					frame = ImageUtility.asBufferedImage(frame.getScaledInstance(1280, totalHeight, Image.SCALE_DEFAULT));
					
					Graphics2D graphicsBackground = frame.createGraphics();
					
					RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					graphicsBackground.setRenderingHints(hints);
					
					int welcomeFontWidth = graphicsBackground.getFontMetrics(discrimFont).stringWidth("Welcome");
					
					graphicsBackground.drawImage(textHolder, 175 + avatarZ, avatarX, null);
				
					graphicsBackground.drawImage(avatarOutline, avatarZ, avatarY, null);
					graphicsBackground.setFont(discrimFont);
					graphicsBackground.setColor(Color.WHITE);
					graphicsBackground.drawString("Welcome", (width-welcomeFontWidth)/2, heightWelcomeText + 40);
					
					return frame;
				});
				
				this.putWelcomerCache(gif, background, entry.getValue().toByteArray());
			}
		}		
		
		Entry<String, ByteArrayOutputStream> entry = ImageUtility.updateEachFrame(new ByteArrayInputStream(this.getWelcomerCache(gif, background)), (frame) -> {
			Graphics2D graphicsBackground = frame.createGraphics();
			
			int fontSize = ImageUtility.getSetSizeText(graphicsBackground, 1025 - graphicsBackground.getFontMetrics(discrimFont).stringWidth(userDiscrim) * 2, Fonts.UNI_SANS, 100, userName);
			Font nameFont = Fonts.UNI_SANS.deriveFont(0, fontSize);
			
			RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphicsBackground.setRenderingHints(hints);
			
			int nameFontWidth = graphicsBackground.getFontMetrics(nameFont).stringWidth(userName);
			
			graphicsBackground.drawImage(ImageUtility.circlify(avatar), avatarZ + 5, avatarY + 5, null);
			graphicsBackground.setFont(nameFont);
			graphicsBackground.drawString(userName, (width-nameFontWidth)/2, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - 20));
			graphicsBackground.setFont(discrimFont);
			graphicsBackground.setColor(new Color(153, 170, 183));
			graphicsBackground.drawString(userDiscrim, (width-nameFontWidth)/2 + nameFontWidth, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - discrimFontHeight) + 30);
			
			return frame;
		});
		
		return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();
	}
	
	@GET
	@Path("/google")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getGoogleImage(@QueryParam("q") String text) throws Exception {	
		Font font = new Font("Arial", 0, 16);
		Color fontColour = new Color(0, 0, 0, 222);
		
		BufferedImage googleImage = ImageIO.read(new File(IMAGE_PATH + "google.png"));
		
		String googleText;
		BufferedImage[] images = new BufferedImage[text.length() + 24];
		for (int i = 0; i < text.length() + 24; i++) {
			BufferedImage clone = ImageUtility.asBufferedImage(googleImage);
			Graphics2D graphics = clone.createGraphics();
			
			RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHints(hints);
			
			graphics.setFont(font);
			graphics.setColor(fontColour);
			
			if (i < text.length()) {				
				googleText = text.substring(0, i + 1);
				
				graphics.drawString(googleText, 378, 335);
				graphics.drawString("|", graphics.getFontMetrics().stringWidth(googleText) + 378, 333);
			} else {
				graphics.drawString(text, 378, 335);
				int num = i - text.length();
				if (num % 6 == 3 || num % 6 == 4 || num % 6 == 5) {
					graphics.drawString("|", graphics.getFontMetrics().stringWidth(text) + 378, 333);
				}
			}
			
			images[i] = clone;
		}
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		GifWriter writer = new GifWriter(output, BufferedImage.TYPE_INT_ARGB, 200, true);
		
		writer.write(googleImage);
		
		for (BufferedImage image : images) {
			writer.write(image);
		}
		
		writer.finish();

		return Response.ok(output.toByteArray()).type("image/gif").build();	
	}
	
	@POST
	@Path("/profile")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getProfileImage(Body body) throws Exception {	
		String profileColour = body.getString("colour");
		String userName = body.getString("user_name");
		List<Byte> bytes = body.getList("background", Byte.class);
		String userAvatarUrl = body.getString("user_avatar_url");
		List<String> badges = body.getList("badges", String.class); 
		String birthday = body.getString("birthday");
		String description = body.getString("description");
		String height = body.getString("height");
		String balance = body.getString("balance");
		int reputation = body.getInteger("reputation");
		List<String> marriedUsers = body.getList("married_users", String.class);
		
		URL userAvatar;
		try {
			userAvatar = new URL(URLDecoder.decode(userAvatarUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).build();
		}
		
		BufferedImage image = null;
		if (bytes != null) {
			try {
				byte[] byteArray = new byte[bytes.size()];
				for (int i = 0; i < bytes.size(); i++) {
					byteArray[i] = ((Number) bytes.get(i)).byteValue();
				}
				
				image = ImageIO.read(new ByteArrayInputStream(byteArray));
			} catch (IOException e) {
				return Response.status(400).build();
			}
		}
		
		BufferedImage background = ImageUtility.fillImage(new BufferedImage(2560, 1440, BufferedImage.TYPE_INT_ARGB), image == null ? new Color(114, 137, 218) : new Color(0, 0, 0, 0));
		
		Color colour = Color.decode(profileColour);		 
		BufferedImage avatarOutline = ImageUtility.fillImage(new BufferedImage(470, 470, BufferedImage.TYPE_INT_ARGB), colour);
		BufferedImage namePlate = ImageUtility.fillImage(new BufferedImage(2000, 500, BufferedImage.TYPE_INT_ARGB), new Color(35, 39, 42));		
		BufferedImage statsPlate = ImageUtility.fillImage(new BufferedImage(2000, 150, BufferedImage.TYPE_INT_ARGB), new Color(44, 47, 51)); 		
		BufferedImage badgePlate = ImageUtility.fillImage(new BufferedImage(560, 650, BufferedImage.TYPE_INT_ARGB), new Color(44, 47, 51)); 
		BufferedImage box = ImageUtility.fillImage(new BufferedImage(1000, 600, BufferedImage.TYPE_INT_ARGB), new Color(0, 0, 0, 175));
		
		Font statsFont = Fonts.EXO_REGULAR.deriveFont(0, 45);
		Font titleFont = Fonts.EXO_REGULAR.deriveFont(0, 70);
		
		BufferedImage avatar = ImageUtility.circlify(ImageUtility.asBufferedImage(ImageIO.read(userAvatar).getScaledInstance(450, 450, Image.SCALE_DEFAULT)));
		
		background.setRGB(0, 0, ImageUtility.asRGBA(255, 255, 255, 255));
			
		Graphics2D graphics = background.createGraphics();
		if (image != null) {
			graphics.drawImage(image, 0, 0, null);
		}
		graphics.drawImage(namePlate, 0, 0, null);
		graphics.drawImage(ImageUtility.circlify(avatarOutline), 15, 15, null);
		graphics.drawImage(avatar, 25, 25, null);
		graphics.drawImage(statsPlate, 0, 500, null);
		graphics.drawImage(badgePlate, 2000, 0, null);
		graphics.drawImage(box, 70, 750, null);
		graphics.drawImage(box, 1490, 750, null);
		
		int badgeX = 0, badgeY = 0;
		for (String badgePath : badges) {
			graphics.drawImage(ImageUtility.asBufferedImage(ImageIO.read(new File(IMAGE_PATH + "badges/" + badgePath)).getScaledInstance(100, 100, Image.SCALE_DEFAULT)), 2030 + badgeX, 130 + badgeY, null);
			badgeX += 130;
			if (badgeX >= 520) {
				badgeY += 120;
				badgeX = 0;
			}
		}
			
		graphics.setStroke(new BasicStroke(10));
		graphics.setColor(colour); 
			
		/* Box 1 */
		graphics.drawRect(75, 755, 1000, 600);

		/* Box 2 */
		graphics.drawRect(1495, 755, 1000, 600);

		/* Skeleton */
		graphics.drawLine(0, 505, 2000, 505);
		graphics.drawLine(0, 655, 2560, 655);
		graphics.drawLine(2005, 0, 2005, 650);

		/* Vertical lines */
		graphics.drawLine(500, 510, 500, 650);
		graphics.drawLine(1000, 510, 1000, 650);
		graphics.drawLine(1500, 510, 1500, 650);
			
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHints(hints);
			
		int fontSize = ImageUtility.getSetSizeText(graphics, 1435, Fonts.EXO_REGULAR, 216, userName);
		Font nameFont = Fonts.EXO_REGULAR.deriveFont(0, fontSize);
		graphics.setFont(nameFont);
		int nameFontWidth = graphics.getFontMetrics().stringWidth(userName);
			
		graphics.drawString(userName, (2000-nameFontWidth)/2 + 215, 250 + Math.round(fontSize/4));
		graphics.setFont(statsFont);
		graphics.drawString("Reputation: " + reputation, 20, 590);
		graphics.drawString("Balance: $" + balance, 520, 590);
		graphics.drawString("Birthday: " + birthday, 1020, 590);
		graphics.drawString("Height: " + height, 1520, 590);
			
		String marriedMessage = "";
		if (marriedUsers.isEmpty()) {
			marriedMessage = "No-one :(";
		} else {
			for (String user : marriedUsers) {
				marriedMessage += "• " + user + "\n\n";
			}
		}
			
		ImageUtility.drawText(graphics, marriedMessage, 1515, 915, -13);
		ImageUtility.drawText(graphics, ImageUtility.getNewLinedWidthText(graphics, statsFont, description, 930), 95, 915);
			
		graphics.setFont(titleFont);
		graphics.drawString("Badges", 2160, 90);
		graphics.drawString("Description", 95, 840);
		graphics.drawString("Partners", 1515, 840);
		
		return Response.ok(background).type("image/png").build();	
	}
	
	@GET
	@Path("/colour")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getColourImage(@QueryParam("hex") String hex, @QueryParam("w") int width, @QueryParam("h") int height) {
		width = width == 0 ? 100 : width;
		height = height == 0 ? 100 : height; 
		
		Color colour = Color.decode(hex.startsWith("#") ? hex : "#" + hex);
		
		BufferedImage colourImage;
		try {
			colourImage = ImageUtility.fillImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), colour);
		} catch (Exception e) {
			return Response.status(400).entity("Hex code is invalid").build();
		}
		
		return Response.ok(colourImage).type("image/png").build();
	}	
	
	private enum Status {
		
		OFFLINE("offline", "invisible"),
		IDLE("idle"),
		DND("dnd", "do no disturb"),
		ONLINE("online"),
		STREAMING("streaming");
		
		private final String[] names;
		
		private Status(String... names) {
			this.names = names;
		}
		
		public String[] getNames() {
			return this.names;
		}
		
		public static Status getStatusByName(String name) {
			for (Status status : Status.values()) {
				for (String names : status.getNames()) {
					if (names.equals(name)) {
						return status;
					}
				}
			}
			
			return null;
		}
		
	}
	
	@GET
	@Path("/status")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getStatusImage(@QueryParam("image") String avatarUrl, @QueryParam("status") String statusArgument) {
		Status status = Status.getStatusByName(statusArgument);
		
		Color colour;
		switch (status) {
			case ONLINE:
				colour = new Color(67, 181, 129);
				break;
			case IDLE:
				colour = new Color(250, 166, 26);
				break;
			case DND:
				colour = new Color(240, 71, 71);
				break;
			case OFFLINE:
				colour = new Color(116, 127, 141);
				break;
			case STREAMING:
				colour = new Color(89, 54, 149);
				break;
			default:
				return Response.status(400).entity("Invalid status was given").build();
		}
		
		URL avatar;
		try {
			avatar = new URL(URLDecoder.decode(avatarUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("The provided image url is invalid").build();
		}
		
		Entry<String, ByteArrayOutputStream> entry;
		try {
			entry = ImageUtility.updateEachFrame(avatar, (frame) -> {
				frame = ImageUtility.circlify(ImageUtility.asBufferedImage(frame.getScaledInstance(270, 270, Image.SCALE_DEFAULT)));
				
				Graphics2D graphics = frame.createGraphics();
				graphics.setComposite(AlphaComposite.Src);
				graphics.setColor(new Color(0, 0, 0, 0));
				graphics.fillOval(204, 204, 66, 66);
				graphics.setComposite(AlphaComposite.SrcOver);
				graphics.setColor(colour);
				
				switch (status) {
					case DND:
						graphics.fillOval(213, 213, 48, 48);
						graphics.setComposite(AlphaComposite.Src);
						graphics.setColor(new Color(0, 0, 0, 0));
						graphics.fill(new RoundRectangle2D.Float(217, 232, 40, 10, 12, 12));
						graphics.setComposite(AlphaComposite.SrcOver);
						break;
					case IDLE:
						graphics.fillArc(210, 210, 48, 48, 189, 250);
						graphics.setComposite(AlphaComposite.Src);
						graphics.setColor(new Color(0, 0, 0, 0));
						graphics.fillArc(203, 203, 40, 40, 220, 185);
						break;
					case OFFLINE:
						graphics.fillOval(213, 213, 48, 48);
						graphics.setComposite(AlphaComposite.Src);
						graphics.setColor(new Color(0, 0, 0, 0));
						graphics.fillOval(225, 225, 24, 24);
						graphics.setComposite(AlphaComposite.SrcOver);
						break;
					default:
						graphics.fillOval(213, 213, 48, 48);
						break;
				}
				
				
				return frame;
			});
		} catch (Exception e) {
			return Response.status(400).entity("The provided image url is not an image").build();
		}
		
		return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();
	}	
	
	@GET
	@Path("/commonColour")
	@Produces({"application/json"})
	public Response getMostCommonColour(@QueryParam("image") String imageUrl) {
		URL avatarUrl;
		try {
			avatarUrl = new URL(URLDecoder.decode(imageUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid image/user").build();
		}
		
		BufferedImage avatar;
		try {
			avatar = ImageIO.read(avatarUrl);
		} catch (IOException e) {
			return Response.status(400).entity("The provided url is not an image").build();
		}
		
		Map<Integer, Integer> mostCommon = new HashMap<>();
		for (int y = 0; y < avatar.getHeight(); y++) {
			for (int x = 0; x < avatar.getWidth(); x++) {
				int rgb = avatar.getRGB(x, y);
				if (((rgb >> 24) & 0xFF) != 0) {
					mostCommon.compute(rgb, (key, value) -> value != null ? value + 1 : 1);
				}
			}
		}

		JSONArray json = new JSONArray();
		mostCommon.entrySet().stream()
			.sorted(Entry.comparingByValue(Comparator.reverseOrder()))
			.forEach(entry -> json.put(new JSONObject().put("colour", entry.getKey()).put("pixels", entry.getValue())));
		
		return Response.ok(new JSONObject().put("colours", json).toString()).type("application/json").build();
	}
	
	@GET
	@Path("/scroll")
	@Produces({"text/plain", "image/png"})
	public Response getScrollImage(@QueryParam("text") String text) {
		BufferedImage scrollImage;
		try {
			scrollImage = ImageIO.read(new File(IMAGE_PATH + "scroll-meme.png"));
		} catch (IOException e) {
			return Response.status(400).build();
		}
		
		Font arial = new Font("Arial", 0, 20);
		
		Graphics2D graphics = scrollImage.createGraphics();
		
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHints(hints);
		
		text = ImageUtility.getNewLinedWidthText(graphics, arial, text, 90);
		
		graphics.setColor(Color.BLACK);
		graphics.setFont(arial);
		ImageUtility.drawText(graphics, text, 95, 305);
		
		return Response.ok(scrollImage).type("image/png").build();
	}
	
	@GET
	@Path("/drift")
	@Produces({"text/plain", "image/png"})
	public Response getDriftImage(@QueryParam("leftText") String leftText, @QueryParam("rightText") String rightText, @QueryParam("image") String imageUrl) {
		URL avatarUrl;
		try {
			avatarUrl = new URL(URLDecoder.decode(imageUrl, StandardCharsets.UTF_8));
		} catch (MalformedURLException e) {
			return Response.status(200).entity("Invalid user/image").build();
		}
		
		BufferedImage avatar;
		try {
			avatar = ImageIO.read(avatarUrl);
		} catch (IOException e) {
			return Response.status(200).entity("That url is not an image").build();
		}
		
		avatar = ImageUtility.asBufferedImage(avatar.getScaledInstance(23, 23, Image.SCALE_DEFAULT));
		
		BufferedImage driftImage;
		try {
			driftImage = ImageIO.read(new File(IMAGE_PATH + "drift-meme.png"));
		} catch (IOException e) {
			return Response.status(400).build();
		}
		
		Font arial = new Font("Arial", 0, 20);
		
		Graphics2D graphics = driftImage.createGraphics();
		
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHints(hints);
		
		leftText = ImageUtility.getNewLinedWidthText(graphics, arial, leftText, 80);
		if (rightText != null) {
			rightText = ImageUtility.getNewLinedWidthText(graphics, arial, rightText, 110);
		}
		
		graphics.setColor(Color.WHITE);
		graphics.setFont(arial);
		ImageUtility.drawText(graphics, leftText, 125, 75);
		if (rightText != null) {
			ImageUtility.drawText(graphics, rightText, 265, 75);
		}
		
		graphics.drawImage(avatar, 270, 335, null);
		
		return Response.ok(driftImage).type("image/png").build();
	}
	
}