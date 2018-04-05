package com.aaman.neo4j;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.neo4j.graphdb.Relationship;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

/**
 * JUNGGraph Neo4J Functions
 */
public class GetJung
{

	// This field declares that we need a GraphDatabaseService
	// as context when any procedure in this class is invoked
	@Context
	public GraphDatabaseService db;

	// This gives us a log instance that outputs messages to the
	// standard log, normally found under `data/log/console.log`
	@Context
	public Log log;

	
	/**
	 * pass properties to Jung function
	 * {storeProps:"yes",filePath:"/Users/aamanlamba/",returnType:"json/svg",returnAs:"raw/file"}
	 */
	private static Properties props;
	
	@UserFunction
	@Description("com.aaman.neo4j.getJungFromPaths(nodes,rels,props) - {storeProps:\"yes\",filePath:\"/Users/aamanlamba/\",returnType:\"json/svg\",returnAs:\"raw/file\"}")
	public String getJungFromPaths(@Name("nodes") List<Node> nodes,
									@Name("rels") List<Relationship> rels,
									@Name("props") Map<String,Object> props) {
		//clear and load props
		GetJung.props.clear();
		GetJung.props.putAll(props);
		String resultStr = "";
		
		switch(GetJung.props.getProperty("returnType")) {
			case "json":
				try {
					resultStr = generateJSONGraphFromPaths(nodes,rels);
				} catch (IOException e) {
					return e.getMessage();
				}
				break;
			case "svg":
				try {
					resultStr = generateSVGGraphFromPaths(nodes,rels);
				} catch (IOException e) {
					return e.getMessage();
				}
				break;
			default:
				break;
		}
		return resultStr;
	}
	

	/**
	 * 
	 * @param nodes
	 * @param rels
	 * @return
	 * @throws IOException
	 */
	private String generateJSONGraphFromPaths(List<Node> nodes, List<Relationship> rels) throws IOException {

		String jsonFilePath = GetJung.props.getProperty("filePath");
		jsonFilePath += "/" + UUID.randomUUID().toString()+".json";
		
		// convert Nodes to RootNodes
		Iterator<Node> itNode = nodes.iterator();
		List<RootNode> rtNodeList = new ArrayList<>();
		while(itNode.hasNext()) {
			rtNodeList.add(new RootNode( itNode.next(),
					GetJung.props.getProperty("storeProps")));
		}
	
		return renderJSONGraphFromPaths(loadJungGraphFromPaths(rtNodeList,rels),jsonFilePath);
	}

	/**
	 * 
	 * @param nodes
	 * @param rels

	 * @return
	 * @throws IOException
	 */
	private String generateSVGGraphFromPaths(List<Node> nodes, List<Relationship> rels) throws IOException {

		String svgFilePath = GetJung.props.getProperty("filePath");
		svgFilePath += "/" + UUID.randomUUID().toString()+".svg";
		
		// convert Nodes to RootNodes
		Iterator<Node> itNode = nodes.iterator();
		List<RootNode> rtNodeList = new ArrayList<>();
		while(itNode.hasNext()) {
			rtNodeList.add(new RootNode( itNode.next(),
					GetJung.props.getProperty("storeProps")));
		}
		
		
		return renderSVGGraphFromPaths(loadJungGraphFromPaths(rtNodeList,rels),svgFilePath);
	}

	/**
	 *  Function to create a JUNG DirectedSparseGraph from a collection of Neo4j Nodes & Relationships
	 * @param nodes
	 * @param rels
	 * @return DirectedSparseGraph<RootNode, RootRelationship
	 */
	private DirectedSparseGraph<RootNode, RootRelationship> loadJungGraphFromPaths(List<RootNode> nodes,
			List<Relationship> rels) {
		DirectedSparseGraph<RootNode,RootRelationship> graph2 = new DirectedSparseGraph<>();
		// convert Relationships to RootRelationships

		// iterate through Relationships and load graph
		Iterator<Relationship> itrel = rels.iterator();
		while(itrel.hasNext()) {
			Relationship rel = itrel.next();
			//get startNode and endNode and add to graph
			RootNode startNode = new RootNode(rel.getStartNode(),
					GetJung.props.getProperty("storeProps"));
			RootNode endNode = new RootNode(rel.getEndNode(),
					GetJung.props.getProperty("storeProps"));
			
			graph2.addVertex(startNode);
			graph2.addVertex(endNode);
			//add relationship to graph as edge
			// convert Relationships to RootRelationships
	
			RootRelationship rtRel = new RootRelationship( rel,
					GetJung.props.getProperty("storeProps")) ;
			graph2.addEdge(rtRel,startNode,endNode);
		}

		// then iterate through nodes and it should add only 'orphan' nodes
		Iterator<RootNode> itnode = nodes.iterator();
		while(itnode.hasNext()) {
			RootNode node = itnode.next();
			graph2.addVertex(node);
		}

		return graph2;
	}

	/**
	 *  Function to return a JSON-formatted string of nodes with their X,Y ISOMLayout vertices
	 * @param DirectedSparseGraph<RootNode, RootRelationship> g
	 * @return String
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException 
	 */
	private String renderJSONGraphFromPaths(DirectedSparseGraph<RootNode, RootRelationship> g,
			String jsonURI) throws JsonProcessingException, FileNotFoundException {
		String JSONGraph="";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

		ISOMLayout<RootNode,RootRelationship> layout = new ISOMLayout<>(g);
		Dimension viewerDim = new Dimension(800,800);
		Rectangle viewerRect = new Rectangle(viewerDim);
		VisualizationViewer<RootNode,RootRelationship> vv =
				new VisualizationViewer<>(layout, viewerDim);
		GraphElementAccessor<RootNode,RootRelationship> pickSupport = 
				vv.getPickSupport();
		Collection<RootNode> vertices = 
				pickSupport.getVertices(layout, viewerRect);
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		//convert vertices to JSON
		vertices  = getDedupVertices(vertices);
		for (RootNode vertex: vertices) {
			//print JSON version of vertex
			
			vertex.posX = layout.getX(vertex);
			vertex.posY = layout.getY(vertex);
			JSONGraph += objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vertex);
		}
		try(PrintWriter jsonFile = new PrintWriter(jsonURI)){
			jsonFile.write(JSONGraph);
		}
		if(GetJung.props.getProperty("returnAs")=="file")
			return jsonURI;
		return JSONGraph;
	}
	
	/**
	 * Function to remove duplicate nodes from vertices collection
	 * @param vertices
	 * @return
	 */
	private Collection<RootNode> getDedupVertices(Collection<RootNode> vertices) {
		Collection<RootNode> dedupVertices = Collections.emptyList();
		dedupVertices = vertices.stream().distinct().collect(Collectors.toList());;		
		return dedupVertices;
	}

	/**
	 *  Function to return SVG as a String from JUNG graph
	 * @param DirectedSparseGraph<RootNode, RootRelationship> g
	 * @return String
	 * @throws IOException
	 */
	private String renderSVGGraphFromPaths(DirectedSparseGraph<RootNode, RootRelationship> g, String svgPath) throws IOException {

		String svgResult="";

		ISOMLayout<RootNode,RootRelationship> layout = new ISOMLayout<>(g);
		Dimension viewerDim = new Dimension(800,800);
		Rectangle viewerRect = new Rectangle(viewerDim);
	    VisualizationImageServer vs =
	    	      new VisualizationImageServer(
	    	        new ISOMLayout(g), viewerDim);
		GraphElementAccessor<String,String> pickSupport = 
				vs.getPickSupport();
		vs.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vs.setBackground(Color.LIGHT_GRAY);
		// create svg from Visualization
		Properties p = new Properties(); 
		p.setProperty("PageSize","A5"); 
		String svgURI = svgPath;
		File svgOutput = new File(svgURI);
		if(svgOutput.exists())
			svgOutput.delete();
		//write to the file for reference for now
		VectorGraphics vg = new SVGGraphics2D(svgOutput, viewerDim);
		vg.setProperties(p); 
		vg.setBackground(Color.YELLOW);

		vg.startExport(); 
		vs.print(vg); 
		vg.endExport();
		
		ByteArrayInputStream bis =  new ByteArrayInputStream(FileUtils.readFileToByteArray(svgOutput));
		svgResult  = IOUtils.toString(bis,StandardCharsets.UTF_8);
	
	/*
		// The following code adds capability for mouse picking of vertices/edges. Vertices can even be moved!
	    final DefaultModalGraphMouse<String,Number> graphMouse = new DefaultModalGraphMouse<>();
	    vv.setGraphMouse(graphMouse);
	    graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
	 	     //Display visualization for reference for now
		  JFrame frame = new JFrame();
		  frame.getContentPane().add(vv);
		  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		  frame.pack();
		  frame.setVisible(true);
	*/
		if(GetJung.props.getProperty("returnAs")=="file")
			return svgPath;

		return svgResult;   
	}



}