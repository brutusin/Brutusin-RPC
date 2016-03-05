/*
 * Copyright 2016 Ignacio del Valle Alles idelvall@brutusin.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.rpc_chat.actions;

import java.io.File;
import java.io.FileInputStream;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;
import org.brutusin.rpc.http.StreamResult;
import org.brutusin.rpc_chat.topics.Attachment;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class DownloadFileAction extends SafeAction<String, StreamResult> {

    @Override
    public Cacheable<StreamResult> execute(String id) throws Exception {
        File f = new File(SendFileAction.UPLOAD_ROOT, id);
        if (!isValid(f)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        File metaFile = new File(SendFileAction.UPLOAD_ROOT, id + ".info");
        Attachment attachment = JsonCodec.getInstance().parse(Miscellaneous.toString(new FileInputStream(metaFile), "UTF-8"), Attachment.class);
        StreamResult ret = new StreamResult(new MetaDataInputStream(new FileInputStream(f), attachment.getName(), attachment.getContentType(), f.length(), f.lastModified()));
        return Cacheable.forMaxHours(ret, 1);
    }

    private static boolean isValid(File f) {
        if (!f.exists()) {
            return false;
        }
        File parent = f.getParentFile();
        while (parent != null) {
            if (parent.equals(SendFileAction.UPLOAD_ROOT)) {
                return true;
            }
            parent = parent.getParentFile();
        }
        return false;
    }
}
