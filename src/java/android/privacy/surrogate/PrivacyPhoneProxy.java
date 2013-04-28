package android.privacy.surrogate;

import android.content.Context;
import android.content.pm.IPackageManager;
import android.os.ServiceManager;
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyDebugger;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.os.Process;

import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.PhoneSubInfo;
import com.android.internal.telephony.UUSInfo;


import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneBase;


/**
 * Copyright (C) 2012-2013 Stefan Thiele (CollegeDev)
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 */
/**
 * Provides privacy handling for phone
 * @author CollegeDev (Stefan T.)
 * {@hide}
 */

public class PrivacyPhoneProxy extends PhoneProxy{

	private static final String P_TAG = "PrivacyPhoneProxy";
	
	private PrivacySettingsManager pSetMan;
	
	private Context context;
	
	private boolean context_available;
	
	/** This PackageManager is needed to get package name if context is not available*/
	private IPackageManager mPm;
	
	public PrivacyPhoneProxy(PhoneBase mPhone, Context context) { //not sure if context is available, so test it!
		super(mPhone);
		if(context != null){
			this.context = context;
			context_available = true;
		}
		else{
			context_available = false;
		}
		initiate(context_available);
		pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
		PrivacyDebugger.i(P_TAG,"Constructor ready for package: " + context.getPackageName());
	}
	
	/**
	 * Method for initalize variables depends on context is availabe or not
	 * @param ctx_av pass true, if context is available and false if not
	 * {@hide}
	 */
	private void initiate(boolean ctx_av){
		if(ctx_av){
			PrivacyDebugger.i(P_TAG,"Context is available for package:" + context.getPackageName());
		} else{
			PrivacyDebugger.e(P_TAG,"Context is not available for package: " + context.getPackageName());
			mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
		}
	}
	
	
	/**
     * {@hide}
     * @return package names of current process which is using this object or null if something went wrong
     */
    private String[] getPackageName(){
    	try{
    		if(mPm != null){
        		int uid = Process.myUid();
        		String[] package_names = mPm.getPackagesForUid(uid);
        		return package_names;
        	}
    		else{
    			mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    			int uid = Process.myUid();
        		String[] package_names = mPm.getPackagesForUid(uid);
        		return package_names;
    		}
    	}
    	catch(Exception e){
    		PrivacyDebugger.e(P_TAG,"something went wrong with getting package name", e);
    		return null;
    	}
    }
    
    @Override
    public Connection dial(String dialNumber) throws CallStateException{
    	if(context_available){
			PrivacySettings settings = pSetMan.getSettings(context.getPackageName(), -1);
			if(pSetMan != null && settings != null && settings.getPhoneCallSetting() != PrivacySettings.REAL) {
				if(settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.EMPTY, PrivacySettings.DATA_PHONE_CALL, null, null);
				throw new CallStateException();
			}
			else{
				if(settings != null && settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_PHONE_CALL, null, null);
				return super.dial(dialNumber);
			}
		}
    	else{
    		String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
						return super.dial(dialNumber); 
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
						throw new CallStateException();
				}
				
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getPhoneCallSetting() != PrivacySettings.REAL) {
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed) {
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_PHONE_CALL, null, null);
				return super.dial(dialNumber); 
			}
			else {
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.EMPTY, PrivacySettings.DATA_PHONE_CALL, null, null);
				throw new CallStateException();
			}
    	}
    }
    
    @Override
    public Connection dial (String dialNumber, UUSInfo uusInfo) throws CallStateException{
    	if(context_available){
			PrivacySettings settings = pSetMan.getSettings(context.getPackageName(), -1);
			if(pSetMan != null && settings != null && settings.getPhoneCallSetting() != PrivacySettings.REAL) {
				if(settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.EMPTY, PrivacySettings.DATA_PHONE_CALL, null, null);
				throw new CallStateException();
			}
			else{
				if(settings != null && settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_PHONE_CALL, null, null);
				return super.dial(dialNumber, uusInfo);
			}
		}
    	else{
    		String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
						return super.dial(dialNumber, uusInfo);
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
						throw new CallStateException();
				}
			}
				
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getPhoneCallSetting() != PrivacySettings.REAL){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_PHONE_CALL, null, null);
				return super.dial(dialNumber, uusInfo); 
			}
			else{
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_PHONE_CALL, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.EMPTY, PrivacySettings.DATA_PHONE_CALL, null, null);
				throw new CallStateException();
			}
    	}
    }

	@Override
	public CellLocation getCellLocation() {
		int phone_type = super.getPhoneType();
		if(context_available){
			PrivacySettings settings = pSetMan.getSettings(context.getPackageName(), Process.myUid());
			if(pSetMan != null && settings != null && (settings.getLocationNetworkSetting() != PrivacySettings.REAL || settings.getLocationGpsSetting() != PrivacySettings.REAL)){
				if(settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, settings.getLocationNetworkSetting(), PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " BLOCKED for getCellLocation()");
				switch(phone_type){
					case PhoneConstants.PHONE_TYPE_GSM:
						return new GsmCellLocation();
					case PhoneConstants.PHONE_TYPE_CDMA:
						return new CdmaCellLocation();
					case PhoneConstants.PHONE_TYPE_NONE:
						return null;
					case PhoneConstants.PHONE_TYPE_SIP:
						return new CdmaCellLocation();
					default: //just in case, but normally this doesn't get a call!
						return new GsmCellLocation();
				}
			}
			else{
				if(settings != null && settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " ALLOWED for getCellLocation(). Default deny: " + ((settings != null) ? settings.isDefaultDenyObject() : "unknown"));
				return super.getCellLocation();
			}
		}
		else{ //context is not available, go through uid!
			String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
						return super.getCellLocation();
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
						switch(phone_type){
							case PhoneConstants.PHONE_TYPE_GSM:
								return new GsmCellLocation();
							case PhoneConstants.PHONE_TYPE_CDMA:
								return new CdmaCellLocation();
							case PhoneConstants.PHONE_TYPE_NONE:
								return null;
							case PhoneConstants.PHONE_TYPE_SIP:
								return new CdmaCellLocation();
							default: //just in case, but normally this doesn't get a call!
								return new GsmCellLocation();
						}
				}
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && (settings.getLocationNetworkSetting() != PrivacySettings.REAL || settings.getLocationGpsSetting() != PrivacySettings.REAL)){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " ALLOWED for getCellLocation()");
				return super.getCellLocation();
			}
			else{
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, settings.getLocationNetworkSetting(), PrivacySettings.DATA_LOCATION_NETWORK, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " BLOCKED for getCellLocation()");
				switch(phone_type){
					case PhoneConstants.PHONE_TYPE_GSM:
						return new GsmCellLocation();
					case PhoneConstants.PHONE_TYPE_CDMA:
						return new CdmaCellLocation();
					case PhoneConstants.PHONE_TYPE_NONE:
						return null;
					case PhoneConstants.PHONE_TYPE_SIP:
						return new CdmaCellLocation();
					default: //just in case, but normally this doesn't get a call!
						return new GsmCellLocation();
				}
			}
		}
	}
	
	@Override
	public PhoneConstants.DataState getDataConnectionState() {
		if(context_available){
			PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
			if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
				if(settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, settings.getNetworkInfoSetting(), PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " BLOCKED for getDataConnection()");
				return PhoneConstants.DataState.CONNECTING; //it's the best way to tell system that we are connecting
			}
			else{
				if(settings != null && settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " ALLOWED for getDataConnection()");
				return super.getDataConnectionState();
			}
		}
		else{
			String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
						return super.getDataConnectionState(); 
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
						return PhoneConstants.DataState.CONNECTED;
				}
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " ALLOWED for getDataConnection()");
				return super.getDataConnectionState();
			}
			else{
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, settings.getNetworkInfoSetting(), PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " BLOCKED for getDataConnection()");
				return PhoneConstants.DataState.CONNECTED;
			}
		}
	}
	
//	@Override
//	public State getState() {
//		State.
//		return null;
//	}
	
//	@Override
//	public String getPhoneName() {
//		return null;
//	}
	
//	@Override
//	public int getPhoneType() {
//		return 0;
//	}
	
	@Override
	public SignalStrength getSignalStrength() {
		SignalStrength output = new SignalStrength();
		if(context_available){
			PrivacySettings settings = pSetMan.getSettings(context.getPackageName(), Process.myUid());
			if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
				if(settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				else
					pSetMan.notification(context.getPackageName(), 0, settings.getNetworkInfoSetting(), PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " BLOCKED for getSignalStrength()");
				return output;
			}
			else{
				if(settings != null && settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " ALLOWED for getSignalStrength()");
				return super.getSignalStrength();
			}
		}
		else{
			String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
						return super.getSignalStrength();
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
						return output;
				}
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " ALLOWED for getSignalStrength()");
				return super.getSignalStrength();
			}
			else{
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				else
					pSetMan.notification(package_names[package_trace], 0, settings.getNetworkInfoSetting(), PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " BLOCKED for getSignalStrength()");
				return output;
			}
		}
	}
	
//	@Override
//	public IccCard getIccCard() {
//		return null;
//	}

	@Override
	public String getLine1Number() {
	   if(context_available){
		   String packageName = context.getPackageName();
	       PrivacySettings pSet = pSetMan.getSettings(packageName);
	       String output;
	       if (pSet != null && pSet != null && pSet.getLine1NumberSetting() != PrivacySettings.REAL) {
	           output = pSet.getLine1Number(); // can be empty, custom or random
	           if(pSet.isDefaultDenyObject())
	        	   pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, null);
	           else
	        	   pSetMan.notification(packageName, 0, pSet.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, null);
	           PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " BLOCKED for getLine1Number()");
	       } else {
	           output = super.getLine1Number();
	           if(pSet != null && pSet.isDefaultDenyObject())
	        	   pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, null);
	           else
	        	   pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, null);
	           PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " ALLOWED for getLine1Number()");
	       }
	       return output;
	   }
	   else{
		    String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			String output;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null){
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, null, null);
						return super.getLine1Number();
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, null, null);
						return "";
				}
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getLine1NumberSetting() != PrivacySettings.REAL){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				output = super.getLine1Number();
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " ALLOWED for getLine1Number()");
				return output;
			}
			else{
				output = settings.getLine1Number();
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
				else
					pSetMan.notification(package_names[package_trace], 0, settings.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " BLOCKED for getLine1Number()");
				return output;
			}
	   }
	}
	
	/**
	 * Will be handled like the Line1Number.
	 */
	@Override
	public String getLine1AlphaTag() {
		return getLine1Number();
	}
	
	/**
	 * Will be handled like the Line1Number, since voice mailbox numbers often
	 * are similar to the phone number of the subscriber.
	 */
	@Override
	public String getVoiceMailNumber() {
		return getLine1Number();
	}
	
	//will look at this later!
//	@Override
//	public void getNeighboringCids(Message response) {
//		
//	}
	
	@Override
	public String getDeviceId() {
		if(context_available){
			String packageName = context.getPackageName();
	        PrivacySettings pSet = pSetMan.getSettings(packageName);
	        String output;
	        if (pSet != null && pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
	            output = pSet.getDeviceId(); // can be empty, custom or random
	            if(pSet.isDefaultDenyObject())
	            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
	            else
	            	pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
	            PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " BLOCKED for getDeviceId()");
	        } else {
	            output = super.getDeviceId();
	            if(pSet != null && pSet.isDefaultDenyObject())
	            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
	            else
	            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
	            PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " ALLOWED for getDeviceId()");
	        }
	        return output;
		}
		else{
			String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			String output;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, null, null);
						return super.getDeviceId();
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, null, null);
						return "";
				}
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getDeviceIdSetting() != PrivacySettings.REAL){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				output = super.getDeviceId();
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, settings);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, settings);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " ALLOWED for getDeviceId()");
				return output;
			}
			else{
				output = settings.getDeviceId();
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, settings);
				else
					pSetMan.notification(package_names[package_trace], 0, settings.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, settings);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " BLOCKED for getDeviceId()");
				return output;
			}
	   }
	}
	
	/**
	 * Will be handled like the DeviceID.
	 */
	@Override
	public String getDeviceSvn() {
		return getDeviceId();
	}
	
	@Override
	public String getSubscriberId() {
		if(context_available){
			String packageName = context.getPackageName();
	        PrivacySettings pSet = pSetMan.getSettings(packageName);
	        String output;
	        if (pSet != null && pSet != null && pSet.getSubscriberIdSetting() != PrivacySettings.REAL) {
	            output = pSet.getSubscriberId(); // can be empty, custom or random
	            if(pSet.isDefaultDenyObject())
	            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);
	            else
	            	pSetMan.notification(packageName, 0, pSet.getSubscriberIdSetting(), PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);
	            PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " BLOCKED for getSubscriberId()");
	        } else {
	            output = super.getSubscriberId();
	            if(pSet != null && pSet.isDefaultDenyObject())
	            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);   
	            else
	            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);   
	            PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " ALLOWED for getSubscriberId()");
	        }
	        return output;
		}
		else{
			String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			String output;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, null, null);
						return super.getSubscriberId();
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, null, null);
						return "";
				}
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getSubscriberIdSetting() != PrivacySettings.REAL){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				output = super.getSubscriberId();
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, settings);      
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_SUBSCRIBER_ID, output, settings);      
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " ALLOWED for getSubscriberId()");
				return output;
			}
			else{
				output = settings.getSubscriberId();
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, settings);
				else
					pSetMan.notification(package_names[package_trace], 0, settings.getSubscriberIdSetting(), PrivacySettings.DATA_SUBSCRIBER_ID, output, settings);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " BLOCKED for getSubscriberId()");
				return output;
			}
		}
		
	}
	
	/**
	 * Will be handled like the SubscriberID.
	 */
	@Override
	public String getIccSerialNumber() {
		return getSubscriberId();
	}
	/**
	 * Will be handled like the SubscriberID.
	 */
	@Override
	public String getEsn() {
		return getSubscriberId();
	}
	/**
	 * Will be handled like the SubscriberID.
	 */
	@Override
	public String getMeid() {
		return getSubscriberId();
	}
	/**
	 * Will be handled like the SubscriberID.
	 */
	@Override
	public String getMsisdn() {
		return getSubscriberId();
	}
	/**
	 * Will be handled like the DeviceID.
	 */
	@Override
	public String getImei() {
		return getDeviceId();
	}
	
	@Override
	public PhoneSubInfo getPhoneSubInfo(){
		PhoneSubInfo output = new PhoneSubInfo(this);
		return output;
	}
	
	@Override
	public ServiceState getServiceState(){
		ServiceState output;
		if(context_available){
			PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
			if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
				if(settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				else
					pSetMan.notification(context.getPackageName(), 0, settings.getNetworkInfoSetting(), PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " BLOCKED for getServiceState()");
				output = super.getServiceState();
				output.setOperatorName("", "", "");
				output.setOperatorAlphaLong("");
				return output;
			}
			else{
				if(settings != null && settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				PrivacyDebugger.i(P_TAG,"package: " + context.getPackageName() + " ALLOWED for getServiceState()");
				return super.getServiceState();
			}
		}
		else{
			String package_names[] = getPackageName();
			boolean allowed = true;
			PrivacySettings settings = null;
			int package_trace = 0; //initalize default with 0, prevents array index out of bounds exception
			boolean default_deny = false;
			if(package_names == null) {
				switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
					case PrivacySettings.DEFAULT_DENY_REAL:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
						return super.getServiceState();
					case PrivacySettings.DEFAULT_DENY_EMPTY:
					case PrivacySettings.DEFAULT_DENY_RANDOM:
						pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
						output = super.getServiceState();
						output.setOperatorName("", "", "");
						output.setOperatorAlphaLong("");
						return output;
				}
				
			}
			for(int i=0;i<package_names.length;i++){
				settings = pSetMan.getSettings(package_names[i]);
				if(settings != null && settings.isDefaultDenyObject())
					default_deny = true;
				if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
					allowed = false;
					package_trace = i;
					break;
				}
			}
			if(allowed){
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				else
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " ALLOWED for getServiceState()");
				return super.getServiceState();
			}
			else{
				if(default_deny)
					pSetMan.notification(package_names[package_trace], 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				else
					pSetMan.notification(package_names[package_trace], 0, settings.getNetworkInfoSetting(), PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, settings);
				PrivacyDebugger.i(P_TAG,"package: " + package_names[package_trace] + " BLOCKED for getServiceState()");
				output = super.getServiceState();
				output.setOperatorName("", "", "");
				output.setOperatorAlphaLong("");
				return output;
			}
		}
	}
	
	
}
