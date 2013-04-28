package android.privacy.surrogate;

import android.content.Context;
import android.net.sip.SipProfile;
import android.os.Binder;
import android.os.Process;
import android.os.ServiceManager;
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyDebugger;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.PhoneSubInfo;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.sip.SipPhone;

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
public class PrivacySipPhone extends SipPhone{

	private static final String P_TAG = "PrivacyGSMPhone";
	
	private PrivacySettingsManager pSetMan;
	
	private Context context;
	
	public PrivacySipPhone(Context context, PhoneNotifier pN, SipProfile sP) {
		super(context, pN, sP); //I've changed the constructor to public!
		this.context = context;
		pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
		PrivacyDebugger.i(P_TAG,"Constructor ready for package: " + context.getPackageName());
	}
	
	@Override
	public String getDeviceSvn() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getDeviceSvn()");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
            output = pSet.getDeviceId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
        } else {
            output = super.getDeviceId();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        }
        return output;
	}
	
	@Override
	public String getImei() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getImei");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
            output = pSet.getDeviceId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
        } else {
            output = super.getImei();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        }
        return output;
	}
	
	@Override
	public String getSubscriberId() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getSubscriberId()");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getSubscriberIdSetting() != PrivacySettings.REAL) {
            output = pSet.getSubscriberId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getSubscriberIdSetting(), PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);
        } else {
            output = super.getSubscriberId();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet); 
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet); 
        }
        return output;
	}
	

//	void notifyLocationChanged() {
//		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for notifyLocationChanged()");
//		PrivacySettings settings = pSetMan.getSettings(context.getPackageName(), Process.myUid());
//		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
//			//do nothing here
//		}
//		else
//			mNotifier.notifyCellLocation(this);
//	}
	
	@Override
	public String getLine1AlphaTag() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getLine1AlphaTag()");
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		String output = "";
		if(pSetMan != null && settings != null && settings.getLine1NumberSetting() != PrivacySettings.REAL){
			output = settings.getLine1Number();
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, settings.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
		}
		else{
			output = super.getLine1AlphaTag();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
		}
		return output;
	}
	
	@Override
	public String getVoiceMailAlphaTag() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getVoiceMailAlphaTag()");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getLine1NumberSetting() != PrivacySettings.REAL) {
            output = pSet.getLine1Number(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        } else {
            output = super.getVoiceMailAlphaTag();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        }
        return output;
	}
	
	public ServiceState getSafeState() {
		PrivacyDebugger.i(P_TAG, "getSafeState()");
		ServiceState mState = super.getServiceState();
		mState.setOperatorName("", "", "");;
		return mState;
	}
	
	@Override
	public String getVoiceMailNumber(){
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getVoiceMailNumber()");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getLine1NumberSetting() != PrivacySettings.REAL) {
            output = pSet.getLine1Number(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        } else {
            output = super.getVoiceMailNumber();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        }
        return output;
	}

	@Override
	public String getDeviceId() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getDeviceId()");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
            output = pSet.getDeviceId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
        } else {
            output = super.getDeviceId();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        }
        return output;
	}
	
	@Override
	public String getMeid() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getMeid()");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
            output = pSet.getDeviceId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
        } else {
            output = super.getMeid();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        }
        return output;
	}
	
	@Override
	public String getEsn() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getEsn()");
		String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
            output = pSet.getDeviceId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
        } else {
            output = super.getEsn();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        }
        return output;
	}
	
	@Override
	public String getLine1Number() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getLine1Number()");
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		String output = "";
		if(pSetMan != null && settings != null && settings.getLine1NumberSetting() != PrivacySettings.REAL){
			output = settings.getLine1Number();
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, settings.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
		}
		else{
			output = super.getLine1Number();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, settings);
		}
		return output;
	}
	
	@Override
	public CellLocation getCellLocation() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getCellLocation()");
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getLocationNetworkSetting() != PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, settings.getLocationNetworkSetting(), PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			return new GsmCellLocation();
		}
		else{
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			return super.getCellLocation();
		}
	}
	
	@Override
	public PhoneSubInfo getPhoneSubInfo() {
		PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getPhoneSubInfo()");
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, settings.getLocationNetworkSetting(), PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			return null;
		}
		else{
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			else
				pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
			return super.getPhoneSubInfo();
		}
	}
	
	@Override
	public ServiceState getServiceState() {
		try{
			PrivacyDebugger.i(P_TAG,"Package: " + context.getPackageName() + " asked for getServiceState()");
			PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
			if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
				if(settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
				else
					pSetMan.notification(context.getPackageName(), 0, settings.getLocationNetworkSetting(), PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
				ServiceState output = super.getServiceState();
				output.setOperatorName("", "", "");
				output.setOperatorAlphaLong("");
				return output;
			}
			else{
				if(settings != null && settings.isDefaultDenyObject())
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
				else
					pSetMan.notification(context.getPackageName(), 0, PrivacySettings.REAL, PrivacySettings.DATA_LOCATION_NETWORK, null, settings);
				return super.getServiceState();
			}
		}
		catch(Exception e){
			PrivacyDebugger.e(P_TAG,"We got exception in getServiceState()-> give fake state", e);
			ServiceState output = super.getServiceState();
			output.setOperatorName("", "", "");
			output.setOperatorAlphaLong("");
			return output;
		}
		
	}
	
	@Override
    public Connection dial(String dialNumber) throws CallStateException{
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName(), -1);
		if(pSetMan != null && settings != null && settings.getPhoneCallSetting() != PrivacySettings.REAL){
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
	
	@Override
    public Connection dial (String dialNumber, UUSInfo uusInfo) throws CallStateException{
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName(), -1);
		if(pSetMan != null && settings != null && settings.getPhoneCallSetting() != PrivacySettings.REAL){
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

}
