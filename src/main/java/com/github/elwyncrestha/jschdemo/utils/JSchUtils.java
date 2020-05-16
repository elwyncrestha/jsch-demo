package com.github.elwyncrestha.jschdemo.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * @author Elvin Shrestha on 5/16/2020
 */
public class JSchUtils {

    private static final Logger log = LoggerFactory.getLogger(JSchUtils.class);

    private static final int SSH_PORT = 22;
    private static final String CHANNEL_SFTP = "sftp";
    private static final String CHANNEL_SHELL = "shell";
    private static final String KNOWN_HOSTS_PATH = "C:/Users/user/.ssh/known_hosts";

    private JSchUtils() {
    }

    public static boolean checkCredentials(String host, String user, String password) {
        boolean connected;
        try {

            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOSTS_PATH);
            Session session = jsch.getSession(user, host, SSH_PORT);
            session.setPassword(password);
            session.connect();
            connected = true;
            session.disconnect();
        } catch (Exception e) {
            connected = false;
        }
        return connected;
    }

    // insecure
    public static boolean checkCredentialsWithNoHostChecking(String host, String user, String password) {
        boolean connected;
        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, SSH_PORT);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            connected = true;
            session.disconnect();
        } catch (Exception e) {
            connected = false;
        }
        return connected;
    }

    public static List<String> getFiles(String host, String user, String password,
        String location) {
        List<String> files = new ArrayList<>();
        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOSTS_PATH);
            Session session = jsch.getSession(user, host, SSH_PORT);
            session.setPassword(password);
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel(CHANNEL_SFTP);
            channelSftp.connect();
            channelSftp.cd(location);
            Vector<LsEntry> list = channelSftp.ls("*.csv");
            list.forEach(f -> files.add(f.getFilename()));

            channelSftp.disconnect();
            session.disconnect();
        } catch (Exception e) {
            log.error("Error getting files: {}", e.getMessage());
            return null;
        }
        return files;
    }

    public static boolean transferFile(String host, String user, String password, String fileFrom, String fileTo) {
        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOSTS_PATH);
            Session session = jsch.getSession(user, host, SSH_PORT);
            session.setPassword(password);
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel(CHANNEL_SFTP);
            channelSftp.connect();
            channelSftp.put(fileFrom, fileTo);

            channelSftp.disconnect();
            session.disconnect();

            return true;
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            return false;
        }
    }

    public static String executeCommand(String host, String user, String password, String command) {
        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOSTS_PATH);
            Session session = jsch.getSession(user, host, SSH_PORT);
            session.setPassword(password);
            session.connect();

            Channel channel = session.openChannel(CHANNEL_SHELL);
            channel.setInputStream(new ByteArrayInputStream(command.getBytes(StandardCharsets.UTF_8)));
            channel.setOutputStream(System.out);
            InputStream in = channel.getInputStream();
            StringBuilder outBuff = new StringBuilder();
            int exitStatus = -1;

            channel.connect();

            while (true) {
                for (int c; ((c = in.read()) >= 0);) {
                    outBuff.append((char) c);
                }

                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    exitStatus = channel.getExitStatus();
                    break;
                }
            }
            channel.disconnect();
            session.disconnect();

            if ( exitStatus == 0 ) {
                return outBuff.toString().trim();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage());
            return null;
        }
    }
}
