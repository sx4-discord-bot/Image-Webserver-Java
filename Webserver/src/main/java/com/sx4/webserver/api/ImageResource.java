package com.sx4.webserver.api;

import static com.sx4.webserver.image.ImageUtility.asBufferedImage;
import static com.sx4.webserver.image.ImageUtility.asRGBA;
import static com.sx4.webserver.image.ImageUtility.circlify;
import static com.sx4.webserver.image.ImageUtility.drawText;
import static com.sx4.webserver.image.ImageUtility.fillImage;
import static com.sx4.webserver.image.ImageUtility.getImageBytes;
import static com.sx4.webserver.image.ImageUtility.getNewLinedText;
import static com.sx4.webserver.image.ImageUtility.getNewLinedWidthText;
import static com.sx4.webserver.image.ImageUtility.getRGBAValue;
import static com.sx4.webserver.image.ImageUtility.getSetSizeText;
import static com.sx4.webserver.image.ImageUtility.rotate;
import static com.sx4.webserver.image.ImageUtility.updateEachFrame;

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
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.jhlabs.image.EmbossFilter;
import com.jhlabs.image.GaussianFilter;
import com.sx4.webserver.Fonts;
import com.sx4.webserver.gif.GifWriter;

@Path("")
public class ImageResource {
	
	private static Random random = new Random();
	
	public static final String IMAGE_PATH = "resources/images/";
	
	private static List<String> statuses = List.of("online", "idle", "dnd", "offline", "streaming");
	
	@GET
	@Path("/resize")
	@Produces({"image/png", "text/plain"})
	public Response getResizedImage(@QueryParam("image") String imageUrl, @QueryParam("height") int height, @QueryParam("width") int width) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(imageUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("That is not a valid url :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				return asBufferedImage(frame.getScaledInstance(width, height, Image.SCALE_DEFAULT));
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage background = new BufferedImage(419, 493, BufferedImage.TYPE_INT_ARGB);
		BufferedImage image = asBufferedImage(ImageIO.read(new File(IMAGE_PATH + "thats-hot-meme.png")).getScaledInstance(419, 493, Image.SCALE_DEFAULT));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				frame = asBufferedImage(frame.getScaledInstance(400, 300, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = background.createGraphics();
				graphics.drawImage(frame, 8, 213, null);
				graphics.drawImage(image, 0, 0, null);
				
				return background;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage flag;
		try {
			flag = asBufferedImage(ImageIO.read(new URL("http://www.geonames.org/flags/x/" + flagQuery + ".gif")).getScaledInstance(200, 200, Image.SCALE_DEFAULT));
		} catch (Exception e) {
			return Response.status(400).entity("Flag initial is invalid :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = asBufferedImage(new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB).getScaledInstance(200, 200, Image.SCALE_DEFAULT));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				frame = asBufferedImage(frame.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = image.createGraphics();
				graphics.drawImage(frame, 0, 0, null);
				Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35F);
				graphics.setComposite(composite);
				graphics.drawImage(flag, 0, 0, null);
				
				return image;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage avatar;
		try {
			avatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The url given is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "trash-meme.jpg"));
		Image resizedAvatar = avatar.getScaledInstance(385, 384, Image.SCALE_DEFAULT);
		
		GaussianFilter filter = new GaussianFilter(20);
		BufferedImage blurredAvatar = filter.filter(asBufferedImage(resizedAvatar), null);
		
		Graphics graphics = image.getGraphics();
		graphics.drawImage(blurredAvatar, 384, 0, null);
		
		return Response.ok(getImageBytes(image)).build();	
	}
	
	@GET
	@Path("/www")
	@Produces({"image/png", "text/plain"})
	public Response getWhoWouldWinImage(@QueryParam("firstImage") String firstQuery, @QueryParam("secondImage") String secondQuery) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(firstQuery, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("First image/user is invalid :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage firstAvatar;
		try {
			firstAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The first url given is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		try {
			url = new URL(URLDecoder.decode(secondQuery, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Second image/user is invalid :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage secondAvatar;
		try {
			secondAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The second url given is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "whowouldwin.png"));
		Image firstResizedAvatar = firstAvatar.getScaledInstance(400, 400, Image.SCALE_DEFAULT);
		Image secondResizedAvatar = secondAvatar.getScaledInstance(400, 400, Image.SCALE_DEFAULT);
		
		Graphics graphics = image.getGraphics();
		graphics.drawImage(firstResizedAvatar, 30, 180, null);
		graphics.drawImage(secondResizedAvatar, 510, 180, null);
		
		return Response.ok(getImageBytes(image)).build();	
	}
	
	@GET
	@Path("/fear")
	@Produces({"image/png", "text/plain"})
	public Response getFearImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "fear-meme.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				frame = asBufferedImage(frame.getScaledInstance(251, 251, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = image.createGraphics();
				graphics.drawImage(frame, 260, 517, null);
				
				return image;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		EmbossFilter filter = new EmbossFilter();
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				BufferedImage embossAvatar = filter.filter(frame, null);
				
				return embossAvatar;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("First user is invalid :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage firstAvatar;
		try {
			firstAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The first url given is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		try {
			url = new URL(URLDecoder.decode(secondQuery, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Second user is invalid :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage secondAvatar;
		try {
			secondAvatar = ImageIO.read(url);
		} catch (Exception e) {
			return Response.status(400).entity("The second url given is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = new BufferedImage(880, 280, BufferedImage.TYPE_INT_ARGB);
		BufferedImage heart = ImageIO.read(new File(IMAGE_PATH + "heart.png"));
		Image firstResizedAvatar = firstAvatar.getScaledInstance(280, 280, Image.SCALE_DEFAULT);
		Image secondResizedAvatar = secondAvatar.getScaledInstance(280, 280, Image.SCALE_DEFAULT);
		
		Graphics graphics = image.getGraphics();
		graphics.drawImage(firstResizedAvatar, 0, 0, null);
		graphics.drawImage(heart, 280, 0, null);
		graphics.drawImage(secondResizedAvatar, 600, 0, null);
		
		return Response.ok(getImageBytes(image)).build();	
	}
	
	@GET
	@Path("/vr")
	@Produces({"image/png", "text/plain"})
	public Response getVrImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}
			
		BufferedImage background = new BufferedImage(493, 511, BufferedImage.TYPE_INT_ARGB);
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "vr.png"));
		Image resizedImage = image.getScaledInstance(493, 511, Image.SCALE_DEFAULT);
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				BufferedImage resizedAvatar = asBufferedImage(frame.getScaledInstance(225, 150, Image.SCALE_DEFAULT));
				
				Graphics2D graphics = background.createGraphics();
				graphics.drawImage(resizedAvatar, 15, 310, null);
				graphics.drawImage(resizedImage, 0, 0, null);
				
				return background;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage background = new BufferedImage(763, 1080, BufferedImage.TYPE_INT_ARGB);
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "shit-meme.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				frame = asBufferedImage(frame.getScaledInstance(192, 192, Image.SCALE_DEFAULT));
	
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
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "gay.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				frame = asBufferedImage(frame);
				BufferedImage resizedImage = asBufferedImage(image.getScaledInstance(frame.getWidth(), frame.getHeight(), Image.SCALE_DEFAULT));
				
				Graphics2D graphics = frame.createGraphics();
				graphics.drawImage(resizedImage, 0, 0, null);
				
				return frame;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user/image :no_entry:").header("Content-Type", "text/plain").build();
		}

		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "beautiful.png"));
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {	
				frame = asBufferedImage(frame.getScaledInstance(90, 104, Image.SCALE_DEFAULT));
				
				frame = rotate(frame, -1);
				
				Graphics2D graphics = image.createGraphics();
				graphics.drawImage(frame, 253, 25, null);
				graphics.drawImage(frame, 256, 222, null);
				
				return image;	
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/discord")
	@Produces({"image/png", "text/plain"})
	public Response getDiscordImage(@QueryParam("text") String query, @QueryParam("theme") String theme, @QueryParam("name") String name, @QueryParam("colour") String colour, @QueryParam("bot") boolean bot, @QueryParam("image") String avatarUrl) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(avatarUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		URL emoteUrl;
		try {
			emoteUrl = new URL(URLDecoder.decode("https://cdn.discordapp.com/emojis/441255212582174731.png", StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid emote :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		Image botImage;
		try {
			botImage = ImageIO.read(emoteUrl).getScaledInstance(60, 60, Image.SCALE_DEFAULT);
		} catch (Exception e) {
			return Response.status(400).entity("The bot emote url is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		int breaks = query.trim().split("\n").length - 1; 
		
		int times = (int) Math.ceil(query.length()/50D);
		
		int height = (breaks * 36) + (times * 36);
		int length = bot ? 66 : 0;
		
		String text = getNewLinedText(query, 50);
		
		Font mainText = Fonts.WHITNEY_BOOK.deriveFont(0, 34);
		Font nameText = Fonts.WHITNEY_MEDIUM.deriveFont(0, 40);
		Font timeText = Fonts.WHITNEY_LIGHT.deriveFont(0, 24);
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {		
				frame = circlify(asBufferedImage(frame.getScaledInstance(100, 100, BufferedImage.TYPE_INT_ARGB)));
				
				BufferedImage image = new BufferedImage(1000, 115 + height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				
				RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				graphics.setRenderingHints(hints);
				
				int nameWidth = graphics.getFontMetrics(nameText).stringWidth(name);
				int nameHeight = 40;
				
				graphics.setColor(theme.equals("dark") ? new Color(54, 57, 63) : Color.WHITE);
				graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
				graphics.drawImage(frame, 20, 10, null);
				if (bot) {
					graphics.drawImage(botImage, 170 + nameWidth, 2, null);
				}
				graphics.setColor(theme.equals("white") ? new Color(116, 127, 141) : Color.WHITE);
				graphics.setFont(mainText);
				drawText(graphics, text, 160, nameHeight + 54);
				graphics.setColor(Color.decode("#" + colour));
				graphics.setFont(nameText);
				graphics.drawString(name, 160, 6 + nameHeight);
				graphics.setColor(new Color(122, 125, 130));
				graphics.setFont(timeText);
				graphics.drawString("Today at " + LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm")), 170 + nameWidth + length, (nameHeight/2) - 2 + 24);
				
				return image;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/trump")
	@Produces({"image/png", "text/plain"})
	public Response getTrumpImage(@QueryParam("text") String query) throws Exception {	
		String text = getNewLinedText(query, 70);
		
		Font textFont = new Font("Arial", 0, 25);
		
		BufferedImage image = ImageIO.read(new File(IMAGE_PATH + "trumptweet-meme.png"));
		
		Graphics2D graphics = image.createGraphics();
		
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHints(hints);
		
		graphics.setColor(Color.BLACK);
		graphics.setFont(textFont);
		drawText(graphics, text, 60, 150);
		
		return Response.ok(getImageBytes(image)).build();
	}
	
	@POST
	@Path("/tweet")
	@Consumes("application/json")
	@Produces({"image/png", "text/plain"})
	@SuppressWarnings("unchecked")
	public Response getTweetImage(Map<String, Object> body) throws Exception {
		String displayName = (String) body.get("displayName");
		String tagName = (String) body.get("name");
		String avatarUrl = (String) body.get("avatarUrl");
		List<String> likeAvatarUrls = (List<String>) body.get("urls");
		int likes = (int) body.get("likes");
		int retweets = (int) body.get("retweets");
		String text = (String) body.get("text");
		
		URL url;
		try {
			url = new URL(avatarUrl);
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		Image avatar;
		try {
			avatar = ImageIO.read(url).getScaledInstance(72, 72, Image.SCALE_DEFAULT);
		} catch (Exception e) {
			return Response.status(400).entity("The url given is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}

		List<BufferedImage> likeAvatars = new ArrayList<BufferedImage>();
		for (String av : likeAvatarUrls) {
			try {
				url = new URL(av);
			} catch (Exception e) {
				return Response.status(400).entity("One of the random avatar urls is invalid :no_entry:").header("Content-Type", "text/plain").build();
			}
				
			try {
				likeAvatars.add(circlify(ImageIO.read(url).getScaledInstance(36, 36, Image.SCALE_DEFAULT)));
			} catch (Exception e) {
				likeAvatars.add(circlify(ImageIO.read(new URL("https://cdn.discordapp.com/embed/avatars/" + random.nextInt(5) + ".png")).getScaledInstance(36, 36, Image.SCALE_DEFAULT)));
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

		String[] splitNewLineText = getNewLinedWidthText(graphics, textFont, text, 833).split("\n");
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
		
		graphics.drawImage(circlify(avatar), 60, 44, null);
		
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
		
		return Response.ok(getImageBytes(image)).build();
	}
	
	@GET
	@Path("/christmas")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getChristmasImage(@QueryParam("image") String query) throws Exception {
		URL url;
		try {
			url = new URL(URLDecoder.decode(query, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid user/image url :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		int width = 256;
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {		
				double widthPercent = width/(double) frame.getWidth();
				int height = (int) (frame.getHeight() * widthPercent);
				frame = asBufferedImage(frame.getScaledInstance(width, height, Image.SCALE_DEFAULT));
				
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
						frame.setRGB(w, h, asRGBA(255, 0, 0, getRGBAValue((int) o)));
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
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
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
			return Response.status(400).entity("Invalid user/image url :no_entry:").header("Content-Type", "text/plain").build();
		}
		
		int width = 256;
		
		try {
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(url, (frame) -> {
				double widthPercent = width/(double) frame.getWidth();
				int height = (int) (frame.getHeight() * widthPercent);
				frame = asBufferedImage(frame.getScaledInstance(width, height, Image.SCALE_DEFAULT));
				
				for (int w = 0; w < frame.getWidth(); w++) {
					for (int h = 0; h < frame.getHeight(); h++) {
						int rgb = frame.getRGB(w, h);
						int r = (rgb >> 16) & 0xFF;
						int g = (rgb >> 8) & 0xFF;
						int b = (rgb >> 0) & 0xFF;
						int a = (rgb >> 24) & 0xFF;
						double o = Math.sqrt(Math.pow(r, 2)*0.299 + Math.pow(g, 2)*0.587 + Math.pow(b, 2)*0.114);
						o *= ((o - 102) / 128);
						frame.setRGB(w, h, asRGBA(getRGBAValue((int) o), getRGBAValue((int) ((o - 10) / 2)), 0, a));
					}
				}
				
				return frame;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		} catch (IIOException e) {
			return Response.status(400).entity("That url is not an image :no_entry:").header("Content-Type", "text/plain").build();
		}
	}
	
	@GET
	@Path("/welcomer")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getWelcomerImage(@QueryParam("background") String background, @QueryParam("userAvatar") String userAvatar, @QueryParam("userName") String userFullName) throws Exception {
		URL backgroundUrl = null;
		if (background != null) {
			try {
				backgroundUrl = new URL(URLDecoder.decode(background, StandardCharsets.UTF_8));
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
		
		BufferedImage avatar;
		try {
			avatar = circlify(asBufferedImage(ImageIO.read(userAvatarUrl).getScaledInstance(300, 300, BufferedImage.TYPE_INT_ARGB)));
		} catch (Exception e) {
			return Response.status(400).build();
		}
		
		BufferedImage textHolder = new BufferedImage(1280, 280, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gTextHolder = textHolder.createGraphics();
		gTextHolder.setColor(new Color(0, 0, 0, 200));
		gTextHolder.fillRect(0, 0, textHolder.getWidth(), textHolder.getHeight());
		
		BufferedImage avatarOutlineRectangle = new BufferedImage(310, 310, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = avatarOutlineRectangle.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, avatarOutlineRectangle.getWidth(), avatarOutlineRectangle.getHeight());
		
		BufferedImage avatarOutline = circlify(avatarOutlineRectangle);
		
		String[] userSplit = userFullName.split("#");
		String userName = userSplit[0];
		String userDiscrim = "#" + userSplit[1];
		
		Font discrimFont = Fonts.UNI_SANS.deriveFont(0, 50);
		
		int nameFontHeight = 100;
		int discrimFontHeight = 50;
		int width = 1420, height = 280;
		
		Graphics2D graphicsAvatar = avatarOutline.createGraphics();
		graphicsAvatar.drawImage(circlify(avatar), 5, 5, null);
		
		BufferedImage backgroundImage;
		if (backgroundUrl == null) {
			backgroundImage  = new BufferedImage(1280, totalHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphicsBackground = backgroundImage.createGraphics();
			
			int fontSize = getSetSizeText(graphicsBackground, 1025 - graphicsBackground.getFontMetrics(discrimFont).stringWidth(userDiscrim) * 2, Fonts.UNI_SANS, 100, userName);
			Font nameFont = Fonts.UNI_SANS.deriveFont(0, fontSize);
			
			RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphicsBackground.setRenderingHints(hints);
			
			int nameFontWidth = graphicsBackground.getFontMetrics(nameFont).stringWidth(userName);
			int welcomeFontWidth = graphicsBackground.getFontMetrics(discrimFont).stringWidth("Welcome");
			
			graphicsBackground.drawImage(textHolder, 175 + avatarZ, avatarX, null);
		
			graphicsBackground.drawImage(avatarOutline, avatarZ, avatarY, null);
			graphicsBackground.setFont(discrimFont);
			graphicsBackground.setColor(Color.WHITE);
			graphicsBackground.drawString("Welcome", (width-welcomeFontWidth)/2, heightWelcomeText + 40);
			graphicsBackground.setFont(nameFont);
			graphicsBackground.drawString(userName, (width-nameFontWidth)/2, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - 20));
			graphicsBackground.setFont(discrimFont);
			graphicsBackground.setColor(new Color(153, 170, 183));
			graphicsBackground.drawString(userDiscrim, (width-nameFontWidth)/2 + nameFontWidth, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - discrimFontHeight) + 30);
			
			return Response.ok(getImageBytes(backgroundImage)).type("image/png").build();
		} else {	
			Entry<String, ByteArrayOutputStream> entry = updateEachFrame(backgroundUrl, (frame) -> {
				frame = asBufferedImage(frame.getScaledInstance(1280, totalHeight, Image.SCALE_DEFAULT));
				
				Graphics2D graphicsBackground = frame.createGraphics();
			
				int fontSize = getSetSizeText(graphicsBackground, 1025 - graphicsBackground.getFontMetrics(discrimFont).stringWidth(userDiscrim) * 2, Fonts.UNI_SANS, 100, userName);
				Font nameFont = Fonts.UNI_SANS.deriveFont(0, fontSize);
				
				RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				graphicsBackground.setRenderingHints(hints);
				
				int nameFontWidth = graphicsBackground.getFontMetrics(nameFont).stringWidth(userName);
				int welcomeFontWidth = graphicsBackground.getFontMetrics(discrimFont).stringWidth("Welcome");
				
				graphicsBackground.drawImage(textHolder, 175 + avatarZ, avatarX, null);
			
				graphicsBackground.drawImage(avatarOutline, avatarZ, avatarY, null);
				graphicsBackground.setFont(discrimFont);
				graphicsBackground.setColor(Color.WHITE);
				graphicsBackground.drawString("Welcome", (width-welcomeFontWidth)/2, heightWelcomeText + 40);
				graphicsBackground.setFont(nameFont);
				graphicsBackground.drawString(userName, (width-nameFontWidth)/2, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - 20));
				graphicsBackground.setFont(discrimFont);
				graphicsBackground.setColor(new Color(153, 170, 183));
				graphicsBackground.drawString(userDiscrim, (width-nameFontWidth)/2 + nameFontWidth, avatarX + (height-nameFontHeight)/2 + (nameFontHeight - discrimFontHeight) + 30);
				
				return frame;
			});
			
			return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();	
		}
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
			BufferedImage clone = asBufferedImage(googleImage);
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
	@SuppressWarnings("unchecked")
	public Response getProfileImage(Map<String, Object> body) throws Exception {	
		String profileColour = (String) body.get("colour");
		String userName = (String) body.get("user_name");
		String backgroundPath = (String) body.get("background_path");
		String userAvatarUrl = (String) body.get("user_avatar_url");
		List<String> badges = (List<String>) body.get("badges"); 
		String birthday = (String) body.get("birthday");
		String description = (String) body.get("description");
		String height = (String) body.get("height");
		String balance = (String) body.get("balance");
		int reputation = (int) body.get("reputation");
		List<String> marriedUsers = (List<String>) body.get("married_users");
		
		URL userAvatar;
		try {
			userAvatar = new URL(URLDecoder.decode(userAvatarUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).build();
		}
		
		BufferedImage image;
		try {
			image = ImageIO.read(new URL(backgroundPath));	
		} catch (Exception e) {
			image = null;
		}
		
		BufferedImage background = fillImage(new BufferedImage(2560, 1440, BufferedImage.TYPE_INT_ARGB), image == null ? new Color(114, 137, 218) : new Color(0, 0, 0, 0));
		
		Color colour = Color.decode(profileColour);		 
		BufferedImage avatarOutline = fillImage(new BufferedImage(470, 470, BufferedImage.TYPE_INT_ARGB), colour);
		BufferedImage namePlate = fillImage(new BufferedImage(2000, 500, BufferedImage.TYPE_INT_ARGB), new Color(35, 39, 42));		
		BufferedImage statsPlate = fillImage(new BufferedImage(2000, 150, BufferedImage.TYPE_INT_ARGB), new Color(44, 47, 51)); 		
		BufferedImage badgePlate = fillImage(new BufferedImage(560, 650, BufferedImage.TYPE_INT_ARGB), new Color(44, 47, 51)); 
		BufferedImage box = fillImage(new BufferedImage(1000, 600, BufferedImage.TYPE_INT_ARGB), new Color(0, 0, 0, 175));
		
		Font statsFont = Fonts.EXO_REGULAR.deriveFont(0, 45);
		Font titleFont = Fonts.EXO_REGULAR.deriveFont(0, 70);
		
		BufferedImage avatar = circlify(asBufferedImage(ImageIO.read(userAvatar).getScaledInstance(450, 450, Image.SCALE_DEFAULT)));
		
		background.setRGB(0, 0, asRGBA(255, 255, 255, 255));
			
		Graphics2D graphics = background.createGraphics();
		if (image != null) {
			graphics.drawImage(image, 0, 0, null);
		}
		graphics.drawImage(namePlate, 0, 0, null);
		graphics.drawImage(circlify(avatarOutline), 15, 15, null);
		graphics.drawImage(avatar, 25, 25, null);
		graphics.drawImage(statsPlate, 0, 500, null);
		graphics.drawImage(badgePlate, 2000, 0, null);
		graphics.drawImage(box, 70, 750, null);
		graphics.drawImage(box, 1490, 750, null);
		
		int badgeX = 0, badgeY = 0;
		for (String badgePath : badges) {
			graphics.drawImage(asBufferedImage(ImageIO.read(new File(IMAGE_PATH + "badges/" + badgePath)).getScaledInstance(100, 100, Image.SCALE_DEFAULT)), 2030 + badgeX, 130 + badgeY, null);
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
			
		int fontSize = getSetSizeText(graphics, 1435, Fonts.EXO_REGULAR, 216, userName);
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
			
		drawText(graphics, marriedMessage, 1515, 915, -13);
		drawText(graphics, getNewLinedWidthText(graphics, statsFont, description, 930), 95, 915);
			
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
			colourImage = fillImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), colour);
		} catch (Exception e) {
			return Response.status(400).entity("Hex code is invalid :no_entry:").build();
		}
		
		return Response.ok(colourImage).type("image/png").build();
	}	
	
	@GET
	@Path("/status")
	@Produces({"image/gif", "text/plain", "image/png"})
	public Response getStatusImage(@QueryParam("image") String avatarUrl, @QueryParam("status") String status) {
		status = status.toLowerCase();
		if (!statuses.contains(status)) {
			return Response.status(400).entity("Invalid status given").build();
		}
		
		Color firstColour = null;
		Color secondColour = null;
		if (status.equals("offline") || status.equals("invisible")) {
			firstColour = Color.decode("#747f8d");
			secondColour = new Color(199, 204, 209);
		} else if (status.equals("dnd")) {
			firstColour = Color.decode("#f04747");
			secondColour = new Color(249, 181, 181);
		} else if (status.equals("idle")) {
			firstColour = Color.decode("#faa61a");
			secondColour = new Color(253, 219, 163);
		} else if (status.equals("online")) {
			firstColour = Color.decode("#43b581");
			secondColour = new Color(180, 225, 205);
		} else if (status.equals("streaming")) {
			firstColour = Color.decode("#593695");
			secondColour = new Color(173, 149, 214);
		}
		
		Color innerStatusColour = firstColour;
		Color outerStatusColour = secondColour;
		
		URL avatar;
		try {
			avatar = new URL(URLDecoder.decode(avatarUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("The provided image url is invalid :no_entry:").build();
		}
		
		Entry<String, ByteArrayOutputStream> entry;
		try {
			entry = updateEachFrame(avatar, (frame) -> {
				frame = circlify(asBufferedImage(frame.getScaledInstance(270, 270, Image.SCALE_DEFAULT)));
				
				Graphics2D graphics = frame.createGraphics();
				graphics.setComposite(AlphaComposite.Src);
				graphics.setColor(new Color(0, 0, 0, 0));
				graphics.fillOval(204, 204, 66, 66);
				graphics.setComposite(AlphaComposite.SrcOver);
				graphics.setColor(outerStatusColour);
				graphics.fillOval(210, 210, 54, 54);
				graphics.setColor(innerStatusColour);
				graphics.fillOval(213, 213, 48, 48);
				
				return frame;
			});
		} catch (Exception e) {
			return Response.status(400).entity("The provided image url is not an image :no_entry:").build();
		}
		
		return Response.ok(entry.getValue().toByteArray()).type("image/" + entry.getKey()).build();
	}	
	
	@GET
	@Path("/commonColour")
	@Produces({"text/plain"})
	public Response getMostCommonColour(@QueryParam("image") String imageUrl) {
		URL avatarUrl;
		try {
			avatarUrl = new URL(URLDecoder.decode(imageUrl, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Response.status(400).entity("Invalid image/user :no_entry:").build();
		}
		
		BufferedImage avatar;
		try {
			avatar = ImageIO.read(avatarUrl);
		} catch (IOException e) {
			return Response.status(400).entity("The provided url is not an image :no_entry:").build();
		}
		
		Map<Integer, Integer> mostCommon = new HashMap<>();
		for (int y = 0; y < avatar.getHeight(); y++) {
			for (int x = 0; x < avatar.getWidth(); x++) {
				int pixelColour = avatar.getRGB(x, y);
				if (mostCommon.containsKey(pixelColour)) {
					mostCommon.put(pixelColour, mostCommon.get(pixelColour) + 1);
				} else {
					mostCommon.put(pixelColour, 1);
				}
			}
		}

		Set<Integer> keys = mostCommon.keySet();
		Integer mostCommonColour = null, mostCommonAmount = null;
		for (Integer key : keys) {
			if (mostCommonColour == null) {
				mostCommonColour = key;
				mostCommonAmount = mostCommon.get(key);
			} else {
				if (mostCommon.get(key) > mostCommonAmount) {
					mostCommonColour = key;
					mostCommonAmount = mostCommon.get(key);
				}
			}
		}
		
		return Response.ok(mostCommonColour).type("text/plain").build();
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
		
		text = getNewLinedWidthText(graphics, arial, text, 90);
		
		graphics.setColor(Color.BLACK);
		graphics.setFont(arial);
		drawText(graphics, text, 95, 305);
		
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
			return Response.status(200).entity("Invalid user/image :no_entry:").build();
		}
		
		BufferedImage avatar;
		try {
			avatar = ImageIO.read(avatarUrl);
		} catch (IOException e) {
			return Response.status(200).entity("That url is not an image :no_entry:").build();
		}
		
		avatar = asBufferedImage(avatar.getScaledInstance(23, 23, Image.SCALE_DEFAULT));
		
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
		
		leftText = getNewLinedWidthText(graphics, arial, leftText, 80);
		if (rightText != null) {
			rightText = getNewLinedWidthText(graphics, arial, rightText, 110);
		}
		
		graphics.setColor(Color.WHITE);
		graphics.setFont(arial);
		drawText(graphics, leftText, 125, 75);
		if (rightText != null) {
			drawText(graphics, rightText, 265, 75);
		}
		
		graphics.drawImage(avatar, 270, 335, null);
		
		return Response.ok(driftImage).type("image/png").build();
	}
	
}