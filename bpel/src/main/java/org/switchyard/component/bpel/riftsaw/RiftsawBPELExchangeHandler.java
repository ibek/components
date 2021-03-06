/* 
 * JBoss, Home of Professional Open Source 
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved. 
 * See the copyright.txt in the distribution for a 
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use, 
 * modify, copy, or redistribute it subject to the terms and conditions 
 * of the GNU Lesser General Public License, v. 2.1. 
 * This program is distributed in the hope that it will be useful, but WITHOUT A 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. 
 * You should have received a copy of the GNU Lesser General Public License, 
 * v.2.1 along with this distribution; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 */
package org.switchyard.component.bpel.riftsaw;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPFault;

import org.apache.log4j.Logger;
import org.riftsaw.engine.BPELEngine;
import org.riftsaw.engine.DeploymentRef;
import org.riftsaw.engine.Fault;
import org.switchyard.BaseHandler;
import org.switchyard.Exchange;
import org.switchyard.ExchangePattern;
import org.switchyard.HandlerException;
import org.switchyard.Message;
import org.switchyard.component.bpel.BPELFault;
import org.switchyard.component.bpel.config.model.BPELComponentImplementationModel;
import org.switchyard.component.bpel.exchange.BPELExchangeHandler;
import org.switchyard.exception.SwitchYardException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A Riftsaw implementation of a BPEL ExchangeHandler.
 *
 */
public class RiftsawBPELExchangeHandler extends BaseHandler implements BPELExchangeHandler {

    private static final String VFS_SCHEME = "vfs";

    private static final String DEPLOY_XML = "deploy.xml";

    private static final Logger LOG =
            Logger.getLogger(RiftsawBPELExchangeHandler.class);

    private BPELEngine _engine = null;
    private QName _wsdlServiceName = null;
    private QName _serviceName = null;
    private javax.wsdl.Definition _wsdl = null;
    private javax.wsdl.PortType _portType = null;
    private static java.util.Map<QName, QName> _serviceRefToCompositeMap=
                new java.util.HashMap<QName, QName>();
    private static java.util.Map<QName, DeploymentRef> _deployed=
                        new java.util.HashMap<QName, DeploymentRef>();

    /**
     * Constructs a new RiftSaw BPEL ExchangeHandler within the specified ServiceDomain.
     *
     */
    public RiftsawBPELExchangeHandler() {
    }

    /**
     * {@inheritDoc}
     */
    public void init(QName qname, BPELComponentImplementationModel model,
                 String intf, BPELEngine engine) {

        _engine = engine;

        _wsdl = WSDLHelper.getWSDLDefinition(intf);

        _portType = WSDLHelper.getPortType(intf, _wsdl);

        javax.wsdl.Service service =
                WSDLHelper.getServiceForPortType(_portType, _wsdl);

        _wsdlServiceName = service.getQName();
        _serviceName = qname;

        // Check if composite is already been initialized for BPEL processes
        QName compositeName = model.getComponent().getComposite().getQName();

        if (!_serviceRefToCompositeMap.containsValue(compositeName)) {

            try {
                java.io.File deployFile=getDeployment();

                DeploymentRef ref=engine.deploy(getDeploymentName(), deployFile);

                _deployed.put(qname, ref);
            } catch (Exception e) {
                throw new SwitchYardException(e);
            }
        }

        _serviceRefToCompositeMap.put(qname, compositeName);
    }

    /**
     * This method returns the deployment name associated with the
     * deployed app.
     * 
     * @return The deployment name
     * @throws Exception Failed to obtain the deployment name
     */
    private String getDeploymentName() throws Exception {
        String ret=null;
        
        java.net.URL url = Thread.currentThread().
                getContextClassLoader().getResource(DEPLOY_XML);
        
        if (url != null) {
            String urlpath=url.toString();
            
            // Remove deploy.xml from end of path, removing an
            // extra character for the path separator
            urlpath = urlpath.substring(0, urlpath.length()-DEPLOY_XML.length()-1);
            
            int fileSeparatorIndex = urlpath.lastIndexOf('/');

            if (fileSeparatorIndex != -1) {
                ret = urlpath.substring(fileSeparatorIndex+1);
                
                int suffixIndex = ret.lastIndexOf('.');
                
                if (suffixIndex != -1) {
                    ret = ret.substring(0, suffixIndex);
                }
            } else {
                LOG.error("Failed to obtain deployment name from URL: "+urlpath);
            }
        } else {
            LOG.error("Unable to locate deployment descriptor (deploy.xml) to derive deployment name");
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.info("Deployment name is: "+ret);
        }
        
        return (ret);
    }
    
    /**
     * This method returns the file associated with the BPEL deployment
     * archive or root folder.
     *
     * @return The deployment
     * @throws Exception Failed to obtain deployment folder/archive
     */
    private java.io.File getDeployment() throws Exception {
        java.io.File ret = null;

        java.net.URL url = Thread.currentThread().
                getContextClassLoader().getResource(DEPLOY_XML);
        int index = url.toString().indexOf(".jar");

        // Check if url contains a jar
        if (index != -1) {

            if (url.getProtocol().equals(VFS_SCHEME)) {
                // AS7 deployment
                try {
                    org.jboss.vfs.VirtualFile vfile=org.jboss.vfs.VFS.getChild(url.toURI());
                    
                    // Recursively get all files
                    java.util.List<org.jboss.vfs.VirtualFile> children=vfile.getParent().getChildrenRecursively();
                    for (org.jboss.vfs.VirtualFile child : children) {
                        // Need to request the physical file to have it expanded
                        // on the file system
                        child.getPhysicalFile();
                    }                   
                    
                    // Virtual file is for the deployment descriptor, so we need
                    // the parent file which represents the root of the deployment
                    ret = vfile.getPhysicalFile().getParentFile();
                    
                } catch (java.lang.NoClassDefFoundError t) {
                    LOG.error("Unable to resolve the deployment URL", t);
                }
                
            } else {
                throw new SwitchYardException(
                        "Unknown deployment environment");
            }
        } else {
            // Retrieve parent folder of deployment descriptor
            ret = new java.io.File(url.toURI()).getParentFile();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deployment=" + ret);
        }

        return (ret);
    }

    /**
     * {@inheritDoc}
     */
    public void start() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("START: " + _serviceName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void handleMessage(final Exchange exchange) throws HandlerException {
        Node request = exchange.getMessage().getContent(Node.class);
        
        java.util.Map<String, Object> headers =
                  new java.util.HashMap<String, Object>();
        
        try {
            // Find part name associated with operation on port type
            javax.wsdl.Operation operation =
                    _portType.getOperation(exchange.getContract().
                            getServiceOperation().getName(),
                                null, null);

            Element newreq =
                    WSDLHelper.wrapRequestMessagePart((Element) request,
                                    operation);

            // Invoke the operation on the BPEL process
            Element response = _engine.invoke(_wsdlServiceName, null,
                    exchange.getContract().
                        getServiceOperation().getName(),
                            newreq, headers);

            if (exchange.getContract().getServiceOperation().
                    getExchangePattern().equals(ExchangePattern.IN_OUT)) {

                Message message = exchange.createMessage();

                // Strip off wrapper and part to just return
                // the part contents
                message.setContent(WSDLHelper.unwrapMessagePart(response));

                exchange.send(message);
            }
        } catch (Fault f) {
            SOAPFault fault = null;

            try {
                fault = javax.xml.soap.SOAPFactory.newInstance().
                        createFault("", f.getFaultName());

                Detail detail=fault.addDetail();
                Node cloned=detail.getOwnerDocument().importNode(WSDLHelper.unwrapMessagePart(f.getFaultMessage()), true);                    
                detail.appendChild(cloned);
        
            } catch (Exception e) {
                throw new HandlerException(e);
            }
            
            throw new BPELFault(fault);
            
        } catch (Exception e) {
            throw new HandlerException(e);
        }
    }

     /**
     * {@inheritDoc}
     */
    public void stop() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("STOP: " + _serviceName);
        }
        
        DeploymentRef ref=_deployed.get(_serviceName);
        
        if (ref != null) {            
            _engine.undeploy(ref);
        }
        
        _serviceRefToCompositeMap.remove(_serviceName);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("DESTROY: " + _serviceName);
        }
    }

}
