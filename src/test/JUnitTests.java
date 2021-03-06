package test;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import common.Constants;
import common.DFileID;

import dfs.DFS;
import dfs.MyDFS;

public class JUnitTests {
	/*
	 * REQUIREMENTS
	 * 
	 *  1. Work correctly with multiple client threads
	 *  2. Free of races, starvation, crashes, deadlocks, storage leaks
	 *  3. Recycle memory and storage correctly
	 *  4. Consistent file model
	 *  	- DFile continues to exist until destroyed
	 *  	- One DFile per DFileID
	 *  	- Read returns data that was most recently written 
	 */
	
	DFS myDFS=MyDFS.getInstance();
	
	@Test
	public void testBasicWriteThenRead() throws FileNotFoundException, IOException{
		myDFS.format();
		DFileID newDFileID = myDFS.createDFile();
		String helloString = "Hello World!";
		byte[] WrittenFromBuffer = helloString.getBytes();
		myDFS.write(newDFileID, WrittenFromBuffer, 0, WrittenFromBuffer.length);
		//HelloWorld String was written to a DFile
		
		byte[] readBuffer = new byte[WrittenFromBuffer.length];
		myDFS.read(newDFileID, readBuffer, 0, readBuffer.length);
		//check that the read buffer equals the buffer we wrote with
		assertTrue(new String(readBuffer).equals(new String(WrittenFromBuffer)));
		
		
		//Now test that multiple DFiles can be stored correctly and read/written from

		
	}

	@Test
	public void testCreateAndDelete() throws FileNotFoundException, IOException{
		myDFS.format();

		int baseSize = myDFS.listAllDFiles().size();
		assertTrue(myDFS.listAllDFiles().size()==baseSize); //myDFS listDFile should be empty at start
		DFileID newDFileID = myDFS.createDFile();
		assertTrue(myDFS.listAllDFiles().size()==baseSize+1); //myDFS should now have 1 DFile
		myDFS.destroyDFile(newDFileID);
		assertTrue(myDFS.listAllDFiles().size()==baseSize); //myDFS should be back to 0 DFiles now
		
		DFileID[] dfileids = new DFileID[Constants.MAX_NUM_FILES];
		for(int i = 0;i<dfileids.length;i++)	dfileids[i]=myDFS.createDFile();
		assertTrue(myDFS.listAllDFiles().size()==Constants.MAX_NUM_FILES); //should exist the max number of dfiles
		
		for(int i = 0;i<dfileids.length;i++){
			myDFS.destroyDFile(dfileids[i]);
		}
		assertTrue(myDFS.listAllDFiles().size()==0); //should be no dfiles now
		
		
		
	}
	
	@Test
	public void testOffset() throws FileNotFoundException, IOException{
		//first test read offset
		myDFS.format();
		String testString = "1234567890";
		byte[] writeFromBuffer = testString.getBytes();

		int readOffset = 5;
		DFileID dfid = myDFS.createDFile();
		myDFS.write(dfid, writeFromBuffer, 0, writeFromBuffer.length);
		byte[] readToBuffer = new byte[writeFromBuffer.length];
		myDFS.read(dfid, readToBuffer, readOffset, readToBuffer.length-readOffset);
		

		for(int i = 0;i<readToBuffer.length-readOffset;i++){
			assertTrue(readToBuffer[i+readOffset]==writeFromBuffer[i]);
		}
		
		
		//test write offset
		DFileID dfid2 = myDFS.createDFile();
		int writeOffset = 3;
		myDFS.write(dfid2, writeFromBuffer, writeOffset, writeFromBuffer.length-writeOffset);
		byte[] readToBuffer2 = new byte[writeFromBuffer.length-writeOffset];
		myDFS.read(dfid2, readToBuffer2, 0, writeFromBuffer.length-writeOffset);
		for(int i = 0;i<readToBuffer2.length;i++){
			assertTrue(readToBuffer2[i]==writeFromBuffer[i+writeOffset]);
		}
	}
	
	/*
	 * Run this test individually to set up the file system for testing for persistence.  Then run only 
	 * testPersistence() individually right afterwards
	 */
	@Ignore
	@Test
	public void setUpTestPersistence() throws IllegalArgumentException, FileNotFoundException, IOException{
		myDFS.format();
		assertTrue(myDFS.listAllDFiles().size()==0);

		byte[] buffer = "asdfasdfasdf".getBytes();
		DFileID dfid = myDFS.createDFile();
		myDFS.write(dfid, buffer, 0, buffer.length);
		assertTrue(myDFS.listAllDFiles().size()==1);
	}
	/*
	 * Run this individually after running setUpTestPersistence()
	 */
	@Ignore
	@Test
	public void testPersistence() throws FileNotFoundException, IOException{
		byte[] buffer =  "asdfasdfasdf".getBytes();
		List<DFileID> dfileids = myDFS.listAllDFiles();
		
		assertTrue(dfileids.size()==1); //there should already and only exist 1 dfile
		
		DFileID firstDFile = dfileids.get(0);
		byte[] readToBuffer = new byte[myDFS.sizeDFile(firstDFile)];
		myDFS.read(firstDFile, readToBuffer, 0, readToBuffer.length);
		System.out.println(new String(readToBuffer));
		System.out.println(new String(buffer));
		assertTrue(new String(buffer).equals(new String(readToBuffer)));
		
		
	}
	
	@Test
	public void testMaxSizeOfDFS() throws FileNotFoundException, IOException{
		
		myDFS.format();
		DFileID[] dfileIDs = new DFileID[Constants.MAX_NUM_FILES];
		//write them all
		for(int i = 0;i<dfileIDs.length;i++){
			dfileIDs[i]=myDFS.createDFile();
			String writeString = "Hello DFS!"+ Integer.toString(i);
			byte[] toWriteBuffer = writeString.getBytes();
			myDFS.write(dfileIDs[i], toWriteBuffer, 0, toWriteBuffer.length);
		}
		
		//now read them all and check equality
		for(int i = 0;i<dfileIDs.length;i++){
			byte[] toReadBuffer = new byte[myDFS.sizeDFile(dfileIDs[i])];
			myDFS.read(dfileIDs[i], toReadBuffer, 0, toReadBuffer.length);
			
			String testString = "Hello DFS!"+Integer.toString(i);
			byte[] equalityBuffer = testString.getBytes();
			assertTrue(new String(toReadBuffer).equals(new String(equalityBuffer)));
		}
		
		
	}
	

	
	@Test
	public void testConcurrentReadingClients(){
		myDFS.format();
		ArrayList<Integer> completeCounter = new ArrayList<Integer>();
		int numReaderThreads = 50;
		for(int i = 0;i<numReaderThreads;i++){
			ReaderClient r = new ReaderClient(myDFS, completeCounter);
			r.run();
		}
		while(completeCounter.size()<numReaderThreads){
			
		}
		assertTrue(completeCounter.size()==numReaderThreads);
		//check that they all finished with no deadlocks
	}
	

	@Test
	public void testAsynchronousWritingClients() throws InterruptedException{
		myDFS.format();
		ArrayList<Integer> completeCounter = new ArrayList<Integer>();

		int numWriterThreads = 50;

		for(int i = 0;i<numWriterThreads;i++){
			WriterClient w = new WriterClient(i, myDFS, completeCounter);
			w.run();
		}
		/*while(completeCounter.size()<numWriterThreads){
			
		}*/

		Thread.sleep(5000);


		System.out.println(completeCounter.size()+" threads have finished out of "+numWriterThreads);
		assertTrue(completeCounter.size()==numWriterThreads);
	}
	

	@Test 
	public void testAsynchronousReadingAndWriting(){
		myDFS.format();
		ArrayList<Integer> completeCounter = new ArrayList<Integer>();
		int numWriterThreads = 25;
		for(int i = 0;i<numWriterThreads;i++){
			WriterClient w = new WriterClient(i, myDFS, completeCounter);
			w.run();
		}
		int numReaderThreads = 25;
		for(int i = 0;i<numReaderThreads;i++){
			ReaderClient r = new ReaderClient(myDFS, completeCounter);
			r.run();
		}
		while(completeCounter.size()<numWriterThreads+numReaderThreads){
			
		}
		assertTrue(completeCounter.size()==numWriterThreads+numReaderThreads);
	}
	
	/*
	 * From the PDF, a file will not exceed 50 blocks, each block is 1024 bytes, so we need 50*1024 bytes
	 */
	@Test
	public void testVeryLargeFiles() throws IllegalArgumentException, FileNotFoundException, IOException{
		
		myDFS.format();
		int twoExponent = 16;
		byte[] bigByteArray = new byte[(int) Math.pow(2, twoExponent)];
		for(int i = 0;i<bigByteArray.length;i++){
			bigByteArray[i]=(byte) ('0'+(i%10));
			}
		DFileID dfid = myDFS.createDFile();
		myDFS.write(dfid, bigByteArray, 0, bigByteArray.length);
		byte[] readToArray = new byte[bigByteArray.length];
		myDFS.read(dfid, readToArray, 0, bigByteArray.length);
		for(int i = 0;i<readToArray.length;i++){
			assertTrue(bigByteArray[i]==readToArray[i]);
		}
		
		
	}

	@Test
	public void testFormat() throws IllegalArgumentException, FileNotFoundException, IOException{
		myDFS.format();
		assertTrue(myDFS.listAllDFiles().size()==0);
		byte[] writeFromBuffer = "adsfasdf".getBytes();
		DFileID dfid = myDFS.createDFile();
		myDFS.write(dfid, writeFromBuffer, 0, writeFromBuffer.length);
		assertTrue(myDFS.listAllDFiles().size()==1);
		System.out.println("before second format");
		myDFS.format();
		
		System.out.println("after second format");
		assertTrue(myDFS.listAllDFiles().size()==0);
	}
	
	/*
	 * Create files that take up the entire VDF space, then delete them all, then add them all again.
	 * Space must be recycled properly for this to work.
	 */
	@Test
	public void testSpaceIsRecycled() throws IllegalArgumentException, FileNotFoundException, IOException{
		myDFS.format();
		DFileID[] dfileIDs = new DFileID[Constants.MAX_NUM_FILES];
		int blocksPerFile = (Constants.NUM_OF_BLOCKS-Constants.BLOCK_OFFSET)/Constants.MAX_NUM_FILES;
		int bytesPerFile = blocksPerFile*Constants.BLOCK_SIZE;
		//write them all
		for(int i = 0;i<dfileIDs.length;i++){
			dfileIDs[i]=myDFS.createDFile();
			
			byte[] toWriteBuffer = new byte[bytesPerFile];
			for(int j = 0;j<toWriteBuffer.length;j++) toWriteBuffer[j]='1';
			myDFS.write(dfileIDs[i], toWriteBuffer, 0, toWriteBuffer.length);
			assertTrue(myDFS.sizeDFile(dfileIDs[i])==bytesPerFile);
		}
		assertTrue(myDFS.listAllDFiles().size()==Constants.MAX_NUM_FILES);
		//delete them all
		for(int i = 0;i<dfileIDs.length;i++){
			myDFS.destroyDFile(dfileIDs[i]);
		}
		assertTrue(myDFS.listAllDFiles().size()==0);
		//now create them all again
		for(int i = 0;i<dfileIDs.length;i++){
			dfileIDs[i]=myDFS.createDFile();
			
			byte[] toWriteBuffer = new byte[bytesPerFile];
			for(int j = 0;j<toWriteBuffer.length;j++) toWriteBuffer[j]='1';
			myDFS.write(dfileIDs[i], toWriteBuffer, 0, toWriteBuffer.length);
			assertTrue(myDFS.sizeDFile(dfileIDs[i])==bytesPerFile);

		}
		assertTrue(myDFS.listAllDFiles().size()==Constants.MAX_NUM_FILES);
	}
	


}
