# jung-neo4j
## JungNeo4J on Neo4J 3.3

Functions to convert Neo4J paths to JUNG graphs in JSON and SVG

A set of user functions to render Neo4J graph data in JUNG visualizations - returned as JSON or SVG (or other formats) for improved performance 

```com.aaman.neo4j.getJungFromPaths(nodes,rels,props,styleProps) - {storeProps:"yes" (Store node & relationship properties in JSON),filePath:"path",returnType:"json/svg",returnAs:"raw/file"},{styleProps:layout:"Tree/FRLayout/ISOM",viewerWidth:"px",viewerHeight:"py"}
```

## Usage example:

```
with [] as nodesColl, [] as relsColl
match p=(person:Person {name:"Keanu Reeves"} )-[:ACTED_IN]->(movie:Movie)
with nodes(p) as pNodes, rels(p) as rNodes,nodesColl,relsColl
unwind pNodes as pN
unwind rNodes as rN
with nodesColl + collect(distinct pN) as nodesColl, relsColl + collect(distinct rN) as relsColl
return com.aaman.neo4j.getJungFromPaths(nodesColl,relsColl,{storeProps:"no",filePath:"/svgFiles",returnType:"svg",returnAs:"file"},{layout:"Tree",viewerWidth:"800",viewerHeight:"800"})
```