/*
 * Copyright 2016 The Trustees of the University of Pennsylvania
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.upenn.library.fcrepo.connector.annex;

import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.NoSuchAlgorithmException;
import org.fcrepo.kernel.api.models.RemoteResource;
import org.modeshape.common.util.SecureHash;

/**
 * 
 * @author magibney
 */
public abstract class AnnexResolver implements RemoteResource {

    private static final int HASH_DIR_DEPTH = 2;
    private static final int HASH_DIR_LENGTH = 3;
    private static final String PREFIX_FORMAT_STRING = "%0"+ (HASH_DIR_DEPTH * HASH_DIR_LENGTH) +"x";

    public static String hashDirPrefix(String annexId, StringBuilder sb) {
        byte[] hash;
        try {
            hash = SecureHash.getHash(SecureHash.Algorithm.MD5, annexId.getBytes(UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        System.err.println(SecureHash.asHexString(hash));
        int start = sb.length();
        appendPrefix(ByteBuffer.wrap(hash, 0, 4).getInt(), sb);
        sb.append(annexId);
        return sb.substring(start);
    }

    public static StringBuilder appendPrefix(int prefix, StringBuilder sb) {
        String rawPrefix = String.format(PREFIX_FORMAT_STRING, prefix >>> 8);
        sb.append(rawPrefix.substring(0, HASH_DIR_LENGTH)).append('/');
        sb.append(rawPrefix.substring(HASH_DIR_LENGTH)).append('/');
        return sb;
    }

    public static void main(String[] args) {
        int blah = 0;
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(blah)).append('\n');
        appendPrefix(blah, sb);
        System.err.println(sb);
    }
    
    public final String annexId;
    private String pathCache;

    protected AnnexResolver(String objectId) {
        this.annexId = objectId;
    }

    public StringBuilder appendPathHashDirLower(StringBuilder sb) {
        if (pathCache != null) {
            return sb.append(pathCache);
        } else {
            pathCache = hashDirPrefix(annexId, sb);
        }
        return sb;
    }

}
