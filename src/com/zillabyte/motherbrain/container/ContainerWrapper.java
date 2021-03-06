package com.zillabyte.motherbrain.container;

import java.io.File;

import org.apache.commons.codec.binary.StringUtils;

import com.zillabyte.motherbrain.flow.Flow;

/****
 * A convenience wrapper
 * @author jake
 *
 */
public class ContainerWrapper implements Container {

  /**
   * 
   */
  private static final long serialVersionUID = 3233736891964331523L;
  
  private Container _delegate;
  
  public void start() throws ContainerException {
    _delegate.start();
  }

  public ContainerExecuteBuilder buildCommand() {
    return _delegate.buildCommand();
  }


  public File getFlowRoot(String flowId) throws ContainerException {
    return getFile(ContainerPathHelper.internalPathForFlow(flowId));
  }
  
  public void writeFile(String internalPath, byte[] contents) throws ContainerException {
    _delegate.writeFile(internalPath, contents);
  }
  
  public void writeFile(String internalPath, String contents) throws ContainerException {
    _delegate.writeFile(internalPath, StringUtils.getBytesUtf8(contents));
  }
  
  public void writeFileInFlowDirectory(String flowId, String name, String contents) throws ContainerException {
    String internalPath = ContainerPathHelper.internalPathForFlow(flowId) + "/" + name;
    _delegate.writeFile(internalPath, StringUtils.getBytesUtf8(contents));
  }
  

  public byte[] readFileAsBytes(String file) throws ContainerException {
    return _delegate.readFileAsBytes(file);
  }
  
  public String readFileAsString(String file) throws ContainerException {
    return StringUtils.newStringUtf8(_delegate.readFileAsBytes(file));
  }
  
  

  public void cleanup() throws ContainerException {
    _delegate.cleanup();
  }

  public ContainerWrapper(Container delegate) {
    _delegate = delegate;
  }

  public Container getDelegate() {
    return _delegate;
  }

  @Override
  public void createDirectory(String path) throws ContainerException {
    _delegate.createDirectory(path);
  }

  @Override
  public File getRoot() {
    return _delegate.getRoot();
  }

  @Override
  public void cacheFlow(Flow flow) {
    _delegate.cacheFlow(flow);
  }

  @Override
  public Flow maybeGetCachedFlow(String id, Integer version) {
    return _delegate.maybeGetCachedFlow(id, version);
  }

  @Override
  public File getFile(String internalPath) throws ContainerException {
    return _delegate.getFile(internalPath);
  }
  
}
