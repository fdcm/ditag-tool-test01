package pt.uninova.ditag.tool.node;

import java.util.HashMap;
import java.util.Map;

public class SchemaNodePair {
	
    private final SchemaNode consumer;
    public SchemaNode getConsumer() {
		return consumer;
	}

    
    private final SchemaNode provider;
	public SchemaNode getProvider() {
		return provider;
	}


	public SchemaNodePair(SchemaNode consumer, SchemaNode provider) {
		super();
		this.consumer = consumer;
		this.provider = provider;
	}
	
	/*
	public Map<String,SchemaNode> toMap() {
		Map<String,SchemaNode> map = new HashMap<String,SchemaNode>();
		
		map.put("consumer", this.consumer);
		map.put("provider", this.provider);
		
        return map;
    }
	*/
	public Map<String,String> toMap() {
		Map<String,String> map = new HashMap<String,String>();
		
		map.put("consumer", this.consumer.getName());
		map.put("provider", this.provider.getName());
		
        return map;
    }
	
	@Override
    public String toString() {
        return this.toMap().toString();
    }
}
