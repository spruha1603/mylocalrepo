package com.comcast.services.dc.staticroute;

import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;


import com.comcast.dc.common.*;
import com.comcast.services.dc.staticroute.namespaces.dcStaticRoute;
import com.tailf.conf.ConfException;
import com.tailf.dp.DpCallbackException;
import com.tailf.dp.annotations.ServiceCallback;
import com.tailf.dp.proto.ServiceCBType;
import com.tailf.dp.services.ServiceContext;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuException;
import com.tailf.navu.NavuList;
import com.tailf.navu.NavuNode;

public class dcStaticRouteRFS {

    private  Logger LOGGER = Logger.getLogger(dcStaticRouteRFS.class);

    /**
     * Create callback method. This method is called when a service instance
     * committed due to a create or update event.
     *
     * This method returns a opaque as a Properties object that can be null. If
     * not null it is stored persistently by Ncs. This object is then delivered
     * as argument to new calls of the create method for this service (fastmap
     * algorithm). This way the user can store and later modify persistent data
     * outside the service model that might be needed.
     *
     * @param context
     *            - The current ServiceContext object
     * @param service
     *            - The NavuNode references the service node.
     * @param ncsRoot
     *            - This NavuNode references the ncs root.
     * @param opaque
     *            - Parameter contains a Properties object. This object may be
     *            used to transfer additional information between consecutive
     *            calls to the create callback. It is always null in the first
     *            call. I.e. when the service is first created.
     * @return Properties the returning opaque instance
     * @throws ConfException
     */

    @ServiceCallback(servicePoint = "dc-static-route", callType = ServiceCBType.CREATE)
    public Properties create(ServiceContext context, NavuNode service,
            NavuNode ncsRoot, Properties opaque) throws ConfException {

        try {
            
                       
            HashMap<String, Object> templateMap = new HashMap<String, Object>();
            String deviceName = service.leaf(dcStaticRoute._device_).valueAsString();
         
            String deviceType = getDeviceType(ncsRoot, deviceName);
            
            templateMap.put("DEVICE", deviceName);
            templateMap.put("SUBNET-MASK", "");
            NavuList ipRouteV4List = service.list(dcStaticRoute._ip_route_v4_);
            for(NavuNode ipRouteV4:ipRouteV4List) {
              String dcStaticRouteIosXrTemplate = "dc-static-route-v4";
              String ipv4Value = ipRouteV4.leaf(dcStaticRoute._dst_prefix_v4_).valueAsString();
              LOGGER.debug("ipv4Value is **************** "+ ipv4Value);
              if("ios-id:cisco-ios".equalsIgnoreCase(deviceType)){
                  String ipAddwithsubnetMask = cidrToSubnetMask(ipv4Value);
                  templateMap.put("SUBNET-MASK", ipAddwithsubnetMask!=null?ipAddwithsubnetMask:"");
              }
              TemplateUtils.applyDeviceConfigIntoTemplates(
                      dcStaticRouteIosXrTemplate, ipRouteV4, context, templateMap);
            }
            
            NavuList ipRouteV6List = service.list(dcStaticRoute._ip_route_v6_);
            for(NavuNode ipRouteV6:ipRouteV6List) {
                String dcStaticRouteIosXrTemplate = "dc-static-route-v6";
                String nodevalue = ipRouteV6.leaf(dcStaticRoute._dst_prefix_v6_).valueAsString();
                LOGGER.debug("Node value is **************** "+ nodevalue);
                TemplateUtils.applyDeviceConfigIntoTemplates(
                        dcStaticRouteIosXrTemplate, ipRouteV6, context, templateMap);
              }

        } catch (NavuException e) {
            throw new DpCallbackException("Cannot create service ", e);
        } catch (DpCallbackException e) {
            throw new DpCallbackException("Error with dc-portchannel service: "
                    + e.getMessage(), e);
        } catch (Exception e) {
            throw new DpCallbackException("Error with dc-portchannel service",
                    e);
        }
        return opaque;

    }
    /**
     * To convert cidr mask to subnet mask.
     * Ex: 10.10.10.1/32 converts to 0.10.10.1/255.255.255.255
     * @param ncsRoot
     * @param deviceName
     * @return
     * @throws DpCallbackException
     */
    private String cidrToSubnetMask(String ipAddress){
        int bits = 32 - Integer.parseInt(ipAddress.substring(ipAddress.indexOf('/')+1));
        int mask = (bits == 32) ? 0 : 0xFFFFFFFF - ((1 << bits)-1); 

        String convertedString = ipAddress.substring(0, ipAddress.indexOf('/') + 1) +
                Integer.toString(mask >> 24 & 0xFF, 10) + "." +
                Integer.toString(mask >> 16 & 0xFF, 10) + "." +
                Integer.toString(mask >>  8 & 0xFF, 10) + "." +
                Integer.toString(mask >>  0 & 0xFF, 10);
        
        return convertedString;
    }
    
    /**
     * To get the device type
     * @param ncsRoot
     * @param deviceName
     * @return
     * @throws DpCallbackException
     */
    public String getDeviceType(NavuNode ncsRoot, String deviceName) throws DpCallbackException {
       
        NavuContainer navuContainer = null;
        String deviceType = null;
        NavuContainer deviceContainer;
        try {
            deviceContainer = ncsRoot.container("devices").list("device").elem(deviceName);
            navuContainer = deviceContainer.container("device-type");

            if (navuContainer.container("cli").exists()) {
                deviceType = navuContainer.container("cli").leaf("ned-id").valueAsString();
                LOGGER.debug("device type " + deviceType);
            } 
        } catch (NavuException e) {
            throw new DpCallbackException("NavuException :", e);
        }
        return deviceType;
    }
}
