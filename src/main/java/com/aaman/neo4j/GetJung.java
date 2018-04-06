package com.aaman.neo4j;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
import com.google.common.base.Function;
import com.google.common.base.Functions;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
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
	 * {storeProps:"yes",filePath:"/Users/aamanlamba/",returnType:"json/svg",returnAs:"raw/file"},{styleProps:layout:"Tree/FRLayout/ISOM,viewerWidth:"px",viewerHeight:"py"}
	 */
	private static Properties props = new Properties();
	private static Properties styleProps = new Properties();
	
	@UserFunction
	@Description("com.aaman.neo4j.getJungFromPaths(nodes,rels,props,styleProps) - {storeProps:\"yes\",filePath:\"/Users/aamanlamba/\",returnType:\"json/svg\",returnAs:\"raw/file\"}")
	public String getJungFromPaths(@Name("nodes") List<Node> nodes,
									@Name("rels") List<Relationship> rels,
									@Name("props") Map<String,Object> props,
									@Name("styleProps") Map<String, Object> styleProps) {
		//clear and load props
		GetJung.props.clear();
		GetJung.props.putAll(props);
		String resultStr = "";
		GetJung.styleProps.clear();
		GetJung.styleProps.putAll(styleProps);
		
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
		
		switch(GetJung.styleProps.getProperty("layout")) {
		case "ISOM":
		case "FRLayout":
			return renderSVGGraphFromPaths(loadJungGraphFromPaths(rtNodeList,rels),svgFilePath);
		case "Tree":
			return renderSVGTreeGraphFromPaths(L2RTreeLayout(rtNodeList,rels), svgFilePath);
		default:
			return null;
		}
	}

	private String renderSVGTreeGraphFromPaths(VisualizationImageServer vs, String svgFilePath) throws IOException {
		

		String svgResult="";
		Dimension viewerDim = new Dimension(800,800);

		// create svg from Visualization
		Properties p = new Properties(); 
		p.setProperty("PageSize","A5"); 
		String svgURI = svgFilePath;
		File svgOutput = new File(svgURI);
		if(svgOutput.exists())
			svgOutput.delete();
		//write to the file for reference for now
		VectorGraphics vg = new SVGGraphics2D(svgOutput, viewerDim);
		vg.setProperties(p); 
		vg.setBackground(Color.WHITE);

		vg.startExport(); 
		vs.print(vg); 
		vg.endExport();
		
		ByteArrayInputStream bis =  new ByteArrayInputStream(FileUtils.readFileToByteArray(svgOutput));
		svgResult  = IOUtils.toString(bis,StandardCharsets.UTF_8);
		
		String returnAs = GetJung.props.getProperty("returnAs");
		switch(returnAs) {
		case "raw":
			return svgResult;
		case "file":
			return svgFilePath;
		default:
			return "";
		}
		

	}
	
	/**
	 * Function to load and create a Left-to-Right Tree Layout
	 * @param rels 
	 * @param rtNodeList 
	 */
	private VisualizationImageServer<RootNode, RootRelationship> L2RTreeLayout(List<RootNode> rtNodeList, List<Relationship> rels) {
		   // create a simple graph for the demo
		
		 VisualizationServer.Paintable rings;
		 
		 String root;
		 TreeLayout<RootNode, RootRelationship> treeLayout;
		 
		 RadialTreeLayout<RootNode, RootRelationship> radialLayout;

		 Forest<RootNode,RootRelationship> graph = loadJungTreeGraphFromPaths(rtNodeList,rels);
     
        
        treeLayout = new TreeLayout<RootNode,RootRelationship>(graph);
        radialLayout = new RadialTreeLayout<RootNode,RootRelationship>(graph);
        radialLayout.setSize(new Dimension(600,600));
        VisualizationImageServer<RootNode, RootRelationship> vv2 =  new VisualizationImageServer<RootNode,RootRelationship>(treeLayout, new Dimension(600,600));
        vv2.setBackground(Color.white);
        vv2.getRenderContext().setEdgeShapeTransformer(EdgeShape.quadCurve(graph));
        vv2.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        // add a listener for ToolTips
      
        vv2.getRenderContext().setArrowFillPaintTransformer(Functions.<Paint>constant(Color.lightGray));
        rings = new Rings(vv2,radialLayout,graph);
        
        setLtoR(vv2);
    	vv2.repaint();
		return vv2;
	}
	
	 private void setLtoR(VisualizationImageServer<RootNode, RootRelationship> vv2) {
	    	Layout<RootNode, RootRelationship> layout = vv2.getModel().getGraphLayout();
	    	Dimension d = layout.getSize();
	    	Point2D center = new Point2D.Double(d.width/2, d.height/2);
	    	vv2.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).rotate(-Math.PI/2, center);
	    }
	    
	    class Rings implements VisualizationServer.Paintable {
	    	VisualizationImageServer<RootNode,RootRelationship> vv2;
	    	Collection<Double> depths;
	    	 RadialTreeLayout<RootNode, RootRelationship> radialLayout;
	    	 Forest<RootNode,RootRelationship> graph;
	    	public Rings(VisualizationImageServer<RootNode, RootRelationship> vv22,RadialTreeLayout<RootNode,RootRelationship> rr, Forest<RootNode, RootRelationship> graph) {
	    		radialLayout = rr;
	    		this.graph = graph;
	    		depths = getDepths();
	    		vv2 = vv22;
	    	}
	    	
	    	private Collection<Double> getDepths() {
	    		Set<Double> depths = new HashSet<Double>();
	    		Map<RootNode,PolarPoint> polarLocations = radialLayout.getPolarLocations();
	    		for(RootNode v : graph.getVertices()) {
	    			PolarPoint pp = polarLocations.get(v);
	    			depths.add(pp.getRadius());
	    		}
	    		return depths;
	    	}

			public void paint(Graphics g) {
				g.setColor(Color.lightGray);
			
				Graphics2D g2d = (Graphics2D)g;
				Point2D center = radialLayout.getCenter();

				Ellipse2D ellipse = new Ellipse2D.Double();
				for(double d : depths) {
					ellipse.setFrameFromDiagonal(center.getX()-d, center.getY()-d, 
							center.getX()+d, center.getY()+d);
					Shape shape = vv2.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).transform(ellipse);
					g2d.draw(shape);
				}
			}

			public boolean useTransform() {
				return true;
			}
	    }
	 
	    
		/**
		 *  Function to create a JUNG TreeGraph from a collection of Neo4j Nodes & Relationships
		 * @param nodes
		 * @param rels
		 * @return DirectedSparseGraph<RootNode, RootRelationship
		 */
		private DelegateForest<RootNode,RootRelationship> loadJungTreeGraphFromPaths(List<RootNode> nodes,
				List<Relationship> rels) {
			DelegateForest<RootNode,RootRelationship> graph2 = new DelegateForest<RootNode,RootRelationship>();
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

		Dimension viewerDim = (Dimension) new Dimension(Integer.parseInt((String) GetJung.styleProps.getProperty("viewerWidth")),
														Integer.parseInt((String) GetJung.styleProps.getProperty("viewerHeight")));
		Layout<RootNode, RootRelationship> layout = null;
		switch(GetJung.styleProps.getProperty("layoutType")) {
		case "FRLayout":
			layout = new FRLayout<RootNode,RootRelationship>(g);
			((FRLayout<RootNode,RootRelationship>)layout).setMaxIterations(100);
			Function<RootNode,Point2D> init = new RandomLocationTransformer<RootNode>(viewerDim,0);
			layout.setInitializer(init);
			break;
		case "ISOMLayout":
		default:
			layout = new ISOMLayout<RootNode,RootRelationship>(g);
			break;
		
		}
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
			
			vertex.posX = ((AbstractLayout<RootNode, RootRelationship>) layout).getX(vertex);
			vertex.posY = ((AbstractLayout<RootNode, RootRelationship>) layout).getY(vertex);
			JSONGraph += objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vertex);
		}
		try(PrintWriter jsonFile = new PrintWriter(jsonURI)){
			jsonFile.write(JSONGraph);
		}
		String returnAs = GetJung.props.getProperty("returnAs");
		switch(returnAs) {
		case "raw":
			return JSONGraph;
		case "file":
			return jsonURI;
		default:
			return "";
		}
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
	private String renderSVGGraphFromPaths(DirectedSparseGraph<RootNode, RootRelationship> g, String svgFilePath) throws IOException {

		String svgResult="";

		Dimension viewerDim = (Dimension) new Dimension(Integer.parseInt((String) GetJung.styleProps.getProperty("viewerWidth")),
														Integer.parseInt((String) GetJung.styleProps.getProperty("viewerHeight")));
		Layout<RootNode, RootRelationship> layout = null;
		switch(GetJung.styleProps.getProperty("layoutType")) {
		case "FRLayout":
			layout = new FRLayout<RootNode,RootRelationship>(g);
			((FRLayout<RootNode,RootRelationship>)layout).setMaxIterations(100);
			Function<RootNode,Point2D> init = new RandomLocationTransformer<RootNode>(viewerDim,0);
			layout.setInitializer(init);
			break;
		case "ISOMLayout":
		default:
			layout = new ISOMLayout<RootNode,RootRelationship>(g);
			break;
		
		}
		
		VisualizationImageServer<RootNode,RootRelationship> vs =
	    	      new VisualizationImageServer<RootNode,RootRelationship>(
	    	        layout, viewerDim);
		vs.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vs.setBackground(Color.LIGHT_GRAY);
		// create svg from Visualization
		Properties p = new Properties(); 
		p.setProperty("PageSize","A5"); 
		File svgOutput = new File(svgFilePath);
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
	
		String returnAs = GetJung.props.getProperty("returnAs");
		switch(returnAs) {
		case "raw":
			return svgResult;
		case "file":
			return svgFilePath;
		default:
			return "";
		}  
	}



}