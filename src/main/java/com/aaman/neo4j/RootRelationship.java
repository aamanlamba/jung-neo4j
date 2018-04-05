/**
 * 
 */
package com.aaman.neo4j;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.neo4j.graphdb.Relationship;


/**
 * @author aamanlamba
 * custom Relationship class that stores a map of Neo4j Relationship properties, the Neo4J Relationship ID and type
 */
public class RootRelationship {
	protected String name="";
	protected String id="";
	protected String type="";
	protected Map<String,Object> nRel;
	protected RootRelationship() {
		name="";
		id="";
		type="";
		nRel = null;
	}
	protected RootRelationship(Relationship rel, String props) {

		
		this.name = rel.getType().toString();
		this.type = rel.getType().toString();
		this.id = ((Long)rel.getId()).toString();
		if(props=="yes")
			nRel = rel.getAllProperties();
		else nRel = null;
	}
	protected RootRelationship(String name,String id,String type) {
		this.name=name;
		this.id=id;
		this.type = type;
	}
	protected String getName() {
		return name;
	}
	protected void setName(String name) {
		this.name = name;
	}
	protected String getId() {
		return id;
	}
	protected void setId(String id) {
		this.id = id;
	}
	protected String getType() {
		return type;
	}
	protected void setType(String type) {
		this.type = type;
	}
	
	  @Override
	    public boolean equals(Object o) {

	        if (o == this) return true;
	        if (!(o instanceof RootRelationship)) {
	            return false;
	        }

	        RootRelationship rn = (RootRelationship) o;

	        return new EqualsBuilder()
	                .append(id, rn.id)
	                .isEquals();
	    }
	  
	  @Override
	    public int hashCode() {
	        return new HashCodeBuilder(17, 37)
	                .append(id)
	                .toHashCode();
	    }
}
