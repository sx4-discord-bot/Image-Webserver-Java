package com.sx4.webserver.gif;

import javax.imageio.metadata.IIOMetadataNode;

public class MetadataUtility {
	
	public static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
		int nNodes = rootNode.getLength();
		for(int i = 0; i < nNodes; i++) {
			if(rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
				return ((IIOMetadataNode) rootNode.item(i));
			}
		}
		
		return null;
	}
}