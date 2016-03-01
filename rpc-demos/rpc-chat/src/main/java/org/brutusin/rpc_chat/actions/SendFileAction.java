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

import org.brutusin.rpc_chat.User;
import org.brutusin.rpc_chat.actions.SendFileAction.SendFileInput;
import org.brutusin.rpc_chat.topics.Attachment;
import org.brutusin.rpc_chat.topics.Message;
import org.brutusin.rpc_chat.topics.MessageTopic;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.commons.utils.CryptoUtils;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.annotations.JsonProperty;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.http.HttpActionSupport;
import org.brutusin.rpc.http.UnsafeAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SendFileAction extends UnsafeAction<SendFileInput, Boolean> {

    public static final File UPLOAD_ROOT = new File(System.getProperty("java.io.tmpdir"), "chat-uploads");
    
    private final AtomicInteger counter = new AtomicInteger();

    private MessageTopic topic;

    public MessageTopic getTopic() {
        return topic;
    }

    public void setTopic(MessageTopic topic) {
        this.topic = topic;
    }

    @Override
    public Boolean execute(SendFileInput input) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("Input can no be null");
        }
        MetaDataInputStream[] streams = input.getFiles();
        Attachment[] attachments = new Attachment[streams.length];
        HttpServletRequest request = HttpActionSupport.getInstance().getHttpServletRequest();
        Integer uploader = User.from(request.getSession()).getId();
        for (int i = 0; i < streams.length; i++) {
            MetaDataInputStream is = streams[i];
            attachments[i] = saveStream(uploader, is);
        }
        Message message = new Message();
        message.setTime(System.currentTimeMillis());
        message.setFrom(uploader);
        message.setTo(input.getTo());
        message.setAttachments(attachments);
        return topic.fire(input.getTo(), message);
    }

    private Attachment saveStream(Integer uploader, MetaDataInputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("Input stream can no be null");
        }
        String id = counter.getAndIncrement() + "-" + CryptoUtils.getHashMD5(is.getName() + System.currentTimeMillis()) + "/" + is.getName();

        File dataFile = new File(UPLOAD_ROOT, id);
        Miscellaneous.createDirectory(dataFile.getParent());
        File metadataFile = new File(dataFile.getAbsolutePath() + ".info");
        FileOutputStream fos = new FileOutputStream(dataFile);
        UploadMetadata metaData = new UploadMetadata();
        Attachment attachment = new Attachment();
        attachment.setId(id);
        attachment.setName(is.getName());
        attachment.setContentType(is.getContentType());
        metaData.setAttachment(attachment);
        metaData.setUploader(uploader);
        Miscellaneous.writeStringToFile(metadataFile, JsonCodec.getInstance().transform(metaData), "UTF-8");
        Miscellaneous.pipeSynchronously(is, fos);
        return attachment;

    }

    public static class SendFileInput {

        private Integer to;
        @JsonProperty(required = true)
        private MetaDataInputStream[] files;

        public Integer getTo() {
            return to;
        }

        public void setTo(Integer to) {
            this.to = to;
        }

        public MetaDataInputStream[] getFiles() {
            return files;
        }

        public void setFiles(MetaDataInputStream[] files) {
            this.files = files;
        }
    }
}
