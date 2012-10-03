package com.bigdata.ha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Formatter;

import org.apache.log4j.Logger;

import com.bigdata.journal.IRootBlockView;
import com.bigdata.journal.ha.HAWriteMessage;

/**
 * Wrapper class to handle process log creation and output for HA.
 * 
 * The process log stores the HAWriteMessages and buffers to support reading and
 * reprocessing as part of the HA synchronization protocol.
 * 
 * The writer encapsulates not only the writing of individual messages but also
 * the closing and creation of new files.
 * 
 * @author Martyn Cutcher
 */
public class ProcessLogWriter {

    private static final Logger log = Logger.getLogger(ProcessLogWriter.class);

    /** HA log directory. */
    private final File m_dir;

    /**
     * The root block of the leader at the start of the current write set.
     */
    private IRootBlockView m_rootBlock;

    /** Current write cache block sequence counter. */
    private long m_sequence = 0;

    /** current log file. */
    private File m_log = null;

    /** current output stream. */
    private ObjectOutputStream m_out = null;

    public ProcessLogWriter(final File logDir) {

        m_dir = logDir;

    }

    /**
     * Open an HA log file for the write set starting with the given root block.
     * 
     * @param rootBlock
     *            The root block.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void createLog(final IRootBlockView rootBlock)
            throws FileNotFoundException, IOException {

        if (rootBlock == null)
            throw new IllegalArgumentException();

        this.m_rootBlock = rootBlock;

        m_sequence = 0L;

        /*
         * Format the name of the log file.
         * 
         * Note: The commit counter in the file name should be zero filled to 20
         * digits so we have the files in lexical order in the file system (for
         * convenience).
         */
        final String logFile;
        {

            final StringBuilder sb = new StringBuilder();

            final Formatter f = new Formatter(sb);

            /*
             * Note: We use commitCounter+1 so the file will be labeled by the
             * commit point that will be achieved when that log file is applied
             * to a journal whose current commit point is [commitCounter].
             */

            final long commitCounter = rootBlock.getCommitCounter();

            f.format("%020d.log", (commitCounter + 1));

            logFile = sb.toString();

        }

        m_log = new File(m_dir, logFile);

        // Must delete file if it exists.
        if (m_log.exists() && !m_log.delete()) {

            throw new IOException("Could not delete: file=" + m_log);
            
        }
        
        m_out = new ObjectOutputStream(new FileOutputStream(m_log));

        writeRootBlock(rootBlock);

    }

    /**
     * Write the final root block on the HA log and close the file. This "seals"
     * the file, which now represents the entire write set associated with the
     * commit point in the given root block.
     * 
     * @param rootBlock
     *            The final root block for the write set.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void closeLog(final IRootBlockView rootBlock)
            throws FileNotFoundException, IOException {

        if (rootBlock == null)
            throw new IllegalArgumentException();

        if (rootBlock.getCommitCounter() != this.m_rootBlock.getCommitCounter()) {

            throw new IllegalStateException();

        }

        if (rootBlock.getLastCommitTime() != this.m_rootBlock
                .getLastCommitTime()) {

            throw new IllegalStateException();

        }

        if (rootBlock.getUUID() != this.m_rootBlock.getUUID()) {

            throw new IllegalStateException();

        }

        writeRootBlock(rootBlock);

        flush();

        close();

        reset();
        
    }

    private void writeRootBlock(final IRootBlockView rootBlock)
            throws IOException {

        if (rootBlock == null)
            throw new IllegalArgumentException();

        m_out.write(rootBlock.asReadOnlyBuffer().array());

        if (log.isDebugEnabled())
            log.debug("wrote root block: " + rootBlock);

    }

    /**
     * 
     * @param msg
     * @param data
     */
    public void write(final HAWriteMessage msg, final ByteBuffer data)
            throws IOException {

        if (m_out == null)
            return;

        /*
         * Check if this really is a valid message for this file. If it is not,
         * then close the file and return immediately
         */
        if (m_rootBlock.getCommitCounter() != msg.getCommitCounter())
            return;

        if (m_rootBlock.getLastCommitTime() != msg.getLastCommitTime())
            return;

        if (m_sequence != msg.getSequence())
            return;

        final byte[] array = data.array();

        m_out.writeObject(msg);

        switch(m_rootBlock.getStoreType()) {
        case RW: {

            assert msg.getSize() == array.length;

            // TODO Efficient channel access and write - must flush first?
            m_out.write(data.array());
        }
        case WORM:
            break;
        }

    }

    /**
     * Close the file (does not flush).
     */
    private void close() throws IOException {
        try {
            if (m_out != null) {
                m_out.close();
            }
        } finally {
            reset();
        }
    }

    /**
     * Clear internal fields.
     */
    private void reset() {
        
        m_log = null;
        
        m_out = null;
        
        m_rootBlock = null;

        m_sequence = 0L;
   
    }

    /**
     * When the HA leader commits it must flush the log
     */
    private void flush() throws IOException {
        if (m_out != null) {
            m_out.flush();
        }
    }

    /**
     * On various error conditions we may need to remove the log
     * 
     * @throws IOException
     */
    public void remove() throws IOException {
        try {
            if (m_out != null) {
                m_out.close();
                m_log.delete();
            }
        } finally {
            reset();
        }
    }

    // FIXME write utility to dump one or more log files.
    public static void main(final String[] args) {

    }

}