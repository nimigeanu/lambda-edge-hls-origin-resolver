package com.client167.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;

public class OriginRouting {
	
	AmazonDynamoDB ddb;
	String tableName;
	static WMSLogger logger = WMSLoggerFactory.getLogger(OriginRouting.class);
	
	public OriginRouting (String tableName){
		logger.info("[]OriginRouting: " + tableName);
		try {
			ddb = AmazonDynamoDBClientBuilder.defaultClient();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		logger.info("ddb: " + ddb);
		this.tableName = tableName;
	}
	
	public void put(String streamName, String originHost) {
		logger.info("OriginRouting::put: " + streamName + "  " + originHost);
		HashMap<String, AttributeValue> items = new HashMap<String, AttributeValue>();

		items.put("Stream", new AttributeValue(streamName));
		items.put("Origin", new AttributeValue(originHost)); 
		try {
			PutItemResult result = ddb.putItem(tableName, items); 
			logger.info("result: " + result);
		} catch (ResourceNotFoundException e) {
			logger.error("Error: The table " + tableName + " can't be found.");
		} catch (AmazonServiceException e) {
			logger.error("AmazonServiceException " + e);
			e.printStackTrace(); 
		}

	}
	
	public void clear(String streamName, String originHost) {
		logger.info("OriginRouting::clear: " + streamName + "  " + originHost);
		//first get the value of the item
		HashMap<String,AttributeValue> key = new HashMap<String,AttributeValue>();
	    key.put("Stream", new AttributeValue(streamName));
		GetItemRequest request = new GetItemRequest()
                .withKey(key)
                .withTableName(tableName);
		Map<String,AttributeValue> result = ddb.getItem(request).getItem();
		logger.info("result: " + result);
		if (result != null) {
            Set<String> keys = result.keySet();
            for (String key1 : keys) {
                logger.info(key1 + ": " + result.get(key1).toString()); 
            }
            String origin = result.get("Origin").getS();
            logger.info("origin: " + origin);
            boolean same = origin.equals(originHost);
            logger.info("same: " + same);
            //only delete the value if it's been put by this origin
            //otherwise it has meanwhile been overwritten and it's somebody else's by now; leave it alone
            if (same){
            	HashMap<String, AttributeValue> items = new HashMap<String, AttributeValue>();
        		items.put("Stream", new AttributeValue(streamName));
        		
        		try {
                    ddb.deleteItem(tableName, items);
                } catch (AmazonServiceException e) {
                	logger.error("AmazonServiceException " + e);
                	e.printStackTrace();
                }
            }
        }
		
	}
}
