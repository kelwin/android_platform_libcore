/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.platform;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketImpl;
// begin WITH_TAINT_TRACKING
import dalvik.system.Taint;
// end WITH_TAINT_TRACKING

/**
 * This wraps native code that implements the INetworkSystem interface.
 * Address length was changed from long to int for performance reasons.
 */
final class OSNetworkSystem implements INetworkSystem {
    private static final OSNetworkSystem singleton = new OSNetworkSystem();

    public static OSNetworkSystem getOSNetworkSystem() {
        return singleton;
    }

    private OSNetworkSystem() {
    }

    public native void accept(FileDescriptor serverFd, SocketImpl newSocket,
            FileDescriptor clientFd) throws IOException;

    public native void bind(FileDescriptor fd, InetAddress inetAddress, int port) throws SocketException;

    // begin WITH_TAINT_TRACKING
    public void connect(FileDescriptor fd, InetAddress inetAddress, int port, int timeout) throws SocketException {
    	String addr = inetAddress.getHostName();
    	if (addr != null) {
            fd.port = port;
    	    fd.hasName = true;
    	    fd.name = addr;
            Taint.log("{ \"OpenNet\": { \"desthost\": \"" + fd.name + "\", \"destport\": \"" + fd.port + "\", \"fd\": \"" + fd.id + "\" } }");
    	}
        connectImpl(fd, inetAddress, port, timeout);
    }
    
    public native void connectImpl(FileDescriptor fd, InetAddress inetAddress, int port, int timeout)
            throws SocketException;
    // end WITH_TAINT_TRACKING

    // begin WITH_TAINT_TRACKING
    public boolean connectNonBlocking(FileDescriptor fd, InetAddress inetAddress, int port) throws IOException {
    	String addr = inetAddress.getHostAddress();
    	if (addr != null) {
            fd.port = port;
    	    fd.hasName = true;
    	    fd.name = addr;
            Taint.log("{ \"OpenNet\": { \"desthost\": \"" + fd.name + "\", \"destport\": \"" + fd.port + "\", \"fd\": \"" + fd.id + "\" } }");
    	}
        return connectNonBlockingImpl(fd, inetAddress, port);
    }
    
    public native boolean connectNonBlockingImpl(FileDescriptor fd, InetAddress inetAddress, int port)
            throws IOException;
    // end WITH_TAINT_TRACKING
    
    public native boolean isConnected(FileDescriptor fd, int timeout) throws IOException;

    public native void socket(FileDescriptor fd, boolean stream) throws SocketException;

    public native void disconnectDatagram(FileDescriptor fd) throws SocketException;

    public native InetAddress getSocketLocalAddress(FileDescriptor fd);

    public native int getSocketLocalPort(FileDescriptor fd);

    public native Object getSocketOption(FileDescriptor fd, int opt) throws SocketException;

    public native void listen(FileDescriptor fd, int backlog) throws SocketException;

    public native int read(FileDescriptor fd, byte[] data, int offset, int count)
            throws IOException;

    public native int readDirect(FileDescriptor fd, int address, int count) throws IOException;

    public native int recv(FileDescriptor fd, DatagramPacket packet,
            byte[] data, int offset, int length,
            boolean peek, boolean connected) throws IOException;

    public native int recvDirect(FileDescriptor fd, DatagramPacket packet,
            int address, int offset, int length,
            boolean peek, boolean connected) throws IOException;

    public boolean select(FileDescriptor[] readFDs, FileDescriptor[] writeFDs,
            int numReadable, int numWritable, long timeout, int[] flags)
            throws SocketException {
        if (numReadable < 0 || numWritable < 0) {
            throw new IllegalArgumentException();
        }

        int total = numReadable + numWritable;
        if (total == 0) {
            return true;
        }

        return selectImpl(readFDs, writeFDs, numReadable, numWritable, flags, timeout);
    }

    static native boolean selectImpl(FileDescriptor[] readfd,
            FileDescriptor[] writefd, int cread, int cwirte, int[] flags,
            long timeout);

    // begin WITH_TAINT_TRACKING
    public int send(FileDescriptor fd, byte[] data, int offset, int length,
            int port, InetAddress inetAddress) throws IOException {
        String dstr = new String(data, offset, length);
        fd.hasName = true;
        fd.name = inetAddress.getHostName();
        fd.port = port;
    	int tag = Taint.getTaintByteArray(data);
    	if (tag != Taint.TAINT_CLEAR) {
            String tstr = "0x" + Integer.toHexString(tag);
            dstr = dstr.replace("\n", " ");
            dstr = dstr.replace("\r", " ");
            Taint.log("{ \"DataLeak\": { \"sink\": \"Network\", \"desthost\": \"" + inetAddress.getHostName() + "\", \"destport\": \"" + port + "\", \"tag\": \"" + tstr + "\", \"data\": \"" + Taint.toHex(dstr.getBytes()) + "\" } }");
    	} else
	        Taint.log("{ \"SendNet\": { \"desthost\": \"" + inetAddress.getHostName() + "\", \"destport\": \"" + port + "\", \"data\": \"" + Taint.toHex(dstr.getBytes()) + "\", \"type\": \"UDP\" } }");
    	return sendImpl(fd, data, offset, length, port, inetAddress);
    }
    
    public native int sendImpl(FileDescriptor fd, byte[] data, int offset, int length,
            int port, InetAddress inetAddress) throws IOException;
    // end WITH_TAINT_TRACKING
    
    // FIXME: TaintDroid currently can't check taint for sendDirect
    public native int sendDirect(FileDescriptor fd, int address, int offset, int length,
            int port, InetAddress inetAddress) throws IOException;

	// begin WITH_TAINT_TRACKING
	public void sendUrgentData(FileDescriptor fd, byte value) {
        String dstr = Byte.toString(value);
		String addr = (fd.hasName) ? fd.name : "unknown";
        int port = (fd.hasName) ? fd.port : 0;
        int tag = Taint.getTaintByte(value);
		if (tag != Taint.TAINT_CLEAR) {
	    String tstr = "0x" + Integer.toHexString(tag);
            dstr = dstr.replace("\n", " ");
            dstr = dstr.replace("\r", " ");
            Taint.log("{ \"DataLeak\": { \"sink\": \"Network\", \"desthost\": \"" + addr + "\", \"destport\": \"" + port + "\", \"tag\": \"" + tstr + "\", \"data\": \"" + Taint.toHex(dstr.getBytes()) + "\" } }");
        } else
                Taint.log("{ \"SendNet\": { \"desthost\": \"" + fd.name + "\", \"destport\": \"" + fd.port + "\", \"data\": \"" + Taint.toHex(dstr.getBytes()) + "\" } }");
		sendUrgentDataImpl(fd, value);
	}

    public native void sendUrgentDataImpl(FileDescriptor fd, byte value);
    // end WITH_TAINT_TRACKING

    public native void setInetAddress(InetAddress sender, byte[] address);

    public native void setSocketOption(FileDescriptor fd, int opt, Object optVal)
            throws SocketException;

    public native void shutdownInput(FileDescriptor fd) throws IOException;

    public native void shutdownOutput(FileDescriptor fd) throws IOException;

    public native void close(FileDescriptor fd) throws IOException;

	// begin WITH_TAINT_TRACKING
	public int write(FileDescriptor fd, byte[] data, int offset, int count)
			throws IOException {
        String dstr = new String(data, offset, count);
        String addr = (fd.hasName) ? fd.name : "unknown";
        int port = (fd.hasName) ? fd.port : 0;
		int tag = Taint.getTaintByteArray(data);
		if (tag != Taint.TAINT_CLEAR) {
            String tstr = "0x" + Integer.toHexString(tag);
            dstr = dstr.replace("\n", " ");
            dstr = dstr.replace("\r", " ");
            Taint.log("{ \"DataLeak\": { \"sink\": \"Network\", \"desthost\": \"" + addr + "\", \"destport\": \"" + port + "\", \"tag\": \"" + tstr + "\", \"data\": \"" + Taint.toHex(dstr.getBytes()) + "\" } }");
		} else
            Taint.log("{ \"SendNet\": { \"desthost\": \"" + fd.name + "\", \"destport\": \"" + fd.port + "\", \"data\": \"" + Taint.toHex(dstr.getBytes()) + "\" } }");
		return writeImpl(fd, data, offset, count);
	}
   
    public native int writeImpl(FileDescriptor fd, byte[] data, int offset, int count)
            throws IOException;
    // end WITH_TAINT_TRACKING

    // FIXME: TaintDroid currently cannot check taint for writeDirect
    public native int writeDirect(FileDescriptor fd, int address, int offset, int count)
            throws IOException;
}
