package com.aaman.neo4j;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"name", "id", "type","properties","posX","posY"})
public class RootNode implements NodeInfo{
	
	@JsonProperty("name")
	protected String name;
	@JsonProperty("id")
	protected String id;
	@JsonProperty("type")
	protected String type;
	@JsonProperty("properties")
	protected Map<String,Object> nNode;
	@JsonProperty("posX")
	protected double posX;
	@JsonProperty("posY")
	protected double posY;
	
	protected RootNode() {
		name="";
		id="";
		type="";
		nNode=null;
		posX=0.0;
		posY=0.0;
	}
	protected RootNode(Node n, String storeProps) {
		
		
		if(n.hasProperty("name"))
			this.name = n.getProperty("name").toString();
		else {
			Iterable<Label> itlab = n.getLabels();
			for(Label lab: itlab) {
				//take the first label
				this.name = lab.name();
				break;
			}
		}
		Iterable<Label> itlab = n.getLabels();
		for(Label lab: itlab) {
			//take the first label
			this.type = lab.name();
			break;
		}
		
		this.id = ((Long)n.getId()).toString();
		if(storeProps=="yes")
			this.nNode = n.getAllProperties();
		else
			this.nNode = null;
		this.posX=0.0;
		this.posY=0.0;
	}
	
	public double getPosX() {
		return posX;
	}
	public void setPosX(double posX) {
		this.posX = posX;
	}
	public double getPosY() {
		return posY;
	}
	public void setPosY(double posY) {
		this.posY = posY;
	}
	protected RootNode(String name,String id,String type) {
		this.name=name;
		this.id=id;
		this.type = type;
		this.nNode=null;
	}
	protected String getName() {
		return name;
	}
	
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Map<String, Object> getnNode() {
		return nNode;
	}
	public void setnNode(Map<String, Object> nNode) {
		this.nNode = nNode;
	}
	protected void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	protected void setType(String type) {
		this.type = type;
	}
	
	
	@Override
	public String toString() {
		return name;
		
	}
	
	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof RootNode)) {
            return false;
        }

        RootNode rt = (RootNode) o;

        return new EqualsBuilder()
                .append(id, rt.id)
                .isEquals();
    }
  
  @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }
}
