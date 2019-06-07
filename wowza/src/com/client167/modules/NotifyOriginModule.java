package com.client167.modules;

import com.client167.helpers.OriginRouting;
import com.wowza.util.HTTPUtils;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify;
import com.wowza.wms.stream.IMediaStreamNotify;

public class NotifyOriginModule extends ModuleBase {
	private String originHostname = null;
	private OriginRouting originRouting;
				
	static WMSLogger logger = WMSLoggerFactory.getLogger(NotifyOriginModule.class);
	
	
	public void onAppStart(IApplicationInstance inst){
		logger.info("onAppStart: " + inst);
		String originRoutingTableName = inst.getProperties().getPropertyStr("originRoutingTableName");
		if (originRoutingTableName != null){
			originRouting = new OriginRouting(originRoutingTableName);
		}
		setupPublishListener(inst);
		
		//define origin hostname in app config if not running on EC2
		originHostname = inst.getProperties().getPropertyStr("originHostname");
		if (originHostname == null){
			originHostname = getMetaProperty("public-hostname");
		}
		
		logger.info("originHostname: " + originHostname);
	}
	
	private void setupPublishListener(IApplicationInstance appInstance){
		class MediaStreamListener implements IMediaStreamNotify {
			public void onMediaStreamCreate(IMediaStream stream) {
				logger.info("onMediaStreamCreate: " + stream.getSrc());
				class ActionListener implements IMediaStreamActionNotify {
					public void onPause(IMediaStream stream, boolean isPause, double location) {}
					public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset) {}
					public void onSeek(IMediaStream stream, double location) {}
					public void onStop(IMediaStream stream) {}
					//
					public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {
						logger.info("onPublish: " + stream.getName());
						notifyStrartStream(stream);
					}

					//
					public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {
						logger.info("onUnPublish: " + stream);
						notifyStopStream(stream);
					}
					
					
				}
				stream.addClientListener(new ActionListener());
			}
			public void onMediaStreamDestroy(IMediaStream stream) {}
		}
		appInstance.addMediaStreamListener(new MediaStreamListener());
	}
	
	private void notifyStrartStream(IMediaStream stream) {
        if (originRouting != null){
        	originRouting.put(stream.getName(), originHostname);
        }
        
	}

	private void notifyStopStream(IMediaStream stream) {
		if (originRouting != null){
			originRouting.clear(stream.getName(), originHostname);
		}
	}
	
	private String getMetaProperty(String attribute){
		return new String(HTTPUtils.HTTPRequestToByteArray("http://169.254.169.254/latest/meta-data/" + attribute, "GET", null, null));
	}
}
