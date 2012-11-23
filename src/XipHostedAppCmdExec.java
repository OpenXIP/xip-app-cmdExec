/**
 * Copyright (c) 2009 Washington University in St. Louis. All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nema.dicom.PS3_19.ArrayOfObjectDescriptor;
import org.nema.dicom.PS3_19.ArrayOfObjectLocator;
import org.nema.dicom.PS3_19.ArrayOfUID;
import org.nema.dicom.PS3_19.ArrayOfUUID;
import org.nema.dicom.PS3_19.AvailableData;
import org.nema.dicom.PS3_19.ObjectDescriptor;
import org.nema.dicom.PS3_19.ObjectLocator;
import org.nema.dicom.PS3_19.Patient;
import org.nema.dicom.PS3_19.Rectangle;
import org.nema.dicom.PS3_19.Series;
import org.nema.dicom.PS3_19.State;
import org.nema.dicom.PS3_19.Study;
import org.nema.dicom.PS3_19.UID;
import org.nema.dicom.PS3_19.UUID;

import edu.wustl.xipApplication.application.ApplicationDataManager;
import edu.wustl.xipApplication.application.ApplicationDataManagerFactory;
import edu.wustl.xipApplication.application.ApplicationTerminator;
import edu.wustl.xipApplication.application.WG23Application;
import edu.wustl.xipApplication.applicationGUI.ExceptionDialog;
import edu.wustl.xipApplication.wg23.ApplicationImpl;
import edu.wustl.xipApplication.wg23.OutputAvailableEvent;
import edu.wustl.xipApplication.wg23.OutputAvailableListener;
import edu.wustl.xipApplication.wg23.WG23DataModel;
import edu.wustl.xipApplication.wg23.WG23DataModelImpl;
import edu.wustl.xipApplication.wg23.WG23Listener;

/**
 * @author Lawrence Tarbox
 */
public class XipHostedAppCmdExec extends WG23Application implements WG23Listener, OutputAvailableListener{
	
	State appCurrentState;
	ApplicationDataManager dataMgr;
	Process cmdProcess;
	String executableCommand;
	String executableArgs;
	String executableDir;

	public XipHostedAppCmdExec(URL hostURL, URL appURL, String executable, String execArgs, String execDir) {
		super(hostURL, appURL);				
		final XipHostedAppCmdExec mainApp = this;

		executableCommand = executable;
		executableArgs = execArgs;
		executableDir = execDir;
		
		/*Notify Host application was launched*/							
		dataMgr = ApplicationDataManagerFactory.getInstance();
		ApplicationImpl appImpl = new ApplicationImpl();
		appImpl.addWG23Listener(this);
		setAndDeployApplicationService(appImpl);		
		getClientToHost().notifyStateChanged(State.IDLE);		
		
	}
	
	public static void main(String[] args) {
		try {
			/*args = new String[4];
			args[0] = "--hostURL";
			args[1] = "http://localhost:8090/HostInterface";
			args[2] = "--applicationURL";
			args[3] = "http://localhost:8060/ApplicationInterface";*/	
			System.out.println("Number of parameters: " + args.length);
			for (int i = 0; i < args.length; i++){
				System.out.println(i + ". " + args[i]);
			}
			URL hostURL = null;
			URL applicationURL = null;
			String executable = null;
			String execArgs = "";
			String execDir = null;
			for (int i = 0; i < args.length; i++){
				if (args[i].equalsIgnoreCase("--hostURL")){
					hostURL = new URL(args[i + 1]);
					i++;
				}else if(args[i].equalsIgnoreCase("--applicationURL")){
					applicationURL = new URL(args[i + 1]);
					i++;
				}else if (args[i].equalsIgnoreCase("--executable")){
					executable = args[i + 1];
					i++;
				}else if (args[i].equalsIgnoreCase("--executableDir")){
					execDir = args[i + 1];
					i++;
				} else {
					execArgs = execArgs + " " + args [i];
				}
			}									
			new XipHostedAppCmdExec(hostURL, applicationURL, executable, execArgs, execDir);										
		} catch (MalformedURLException e) {			
			e.printStackTrace();
		} catch (NullPointerException e){
			new ExceptionDialog("List of parameters is not valid!", 
					"Ensure: --hostURL url1 --applicationURL url2 --executable command --executableDir workiingDir",
					"Launch Application Dialog");
			System.exit(0);
		}
	}
	
	public String getSceneGraphInput(List<ObjectLocator> objLocs){
		String input = new String();
		int size = objLocs.size();
		for (int i = 0; i < size; i++){
			if(i == 0){
				String filePath;				
				filePath = new File(objLocs.get(i).getURI()).getPath();
				// input = input + "\"" + nols.get(i).getURI() + "\"" + ", ";					
				filePath = filePath.substring(6 , filePath.length());
				input = filePath + " ";								
			} else if(i < size -1){
				String filePath = new File(objLocs.get(i).getURI()).getPath();
				//input = input + "\"" + nols.get(i).getURI() + "\"" + ", ";
				filePath = filePath.substring(6 , filePath.length());
				input = input + filePath + " ";
			}else if(i == size -1){
				String filePath = new File(objLocs.get(i).getURI()).getPath();
				//input = input + "\"" + nols.get(i).getURI() + "\"" + ", ";
				filePath = filePath.substring(6 , filePath.length());
				input = input + filePath;
			}				
		}
		return input;
	}
	
	@Override
	public boolean bringToFront(Rectangle location) {
		// Schedule a job for the event-dispatching thread:
		// bringing to front.
		return true;
		//return false;
	}

	@Override
	public void notifyDataAvailable(AvailableData availableData,
			boolean lastData) {

		ArrayOfUUID arrayUUIDs = new ArrayOfUUID();
		List<UUID> listUUIDs = arrayUUIDs.getUUID();
		ArrayOfUID arrayTsUID = new ArrayOfUID();
		List<UID> listTsUID = arrayTsUID.getUID();
		HashSet<UID> setTsUID = new HashSet<UID>(5);

		// Extract UUIDs for all objects
		extractUUIDs (availableData.getObjectDescriptors(), listUUIDs, setTsUID);

		if ((availableData.getPatients() != null)
							&& (availableData.getPatients().getPatient() != null)) {
			List<Patient> patients = availableData.getPatients().getPatient();	
			for (Patient patient : patients) {
				if (patient == null) {
					continue;
				}
				extractUUIDs (patient.getObjectDescriptors(), listUUIDs, setTsUID);
				if (patient.getStudies() == null) {
					continue;
				}
				List<Study> studies = patient.getStudies().getStudy();
				if ((studies == null) || (studies.size() <= 0)) {
					continue;
				}
				for (Study study : studies) {
					if (study == null) {
						continue;
					}
					extractUUIDs (study.getObjectDescriptors(), listUUIDs, setTsUID);
					if (study.getSeries() == null) {
						continue;
					}
					List<Series> listOfSeries = study.getSeries().getSeries();
					if ((listOfSeries == null) || (listOfSeries.size() <= 0)) {
						continue;
					}
					for (Series series : listOfSeries) {
						if (series == null) {
							continue;
						}
						extractUUIDs (series.getObjectDescriptors(), listUUIDs, setTsUID);
					}
				}
			}
		}
		if (listUUIDs.isEmpty()) {
			return;
		}

		String defaultTSString = "1.2.840.10008.1.2.1";
		UID defaultTsUID = new UID();
		defaultTsUID.setUid(defaultTSString);
		setTsUID.add(defaultTsUID);
		for (UID tsUID : setTsUID) {
			listTsUID.add(tsUID);
		}
		ArrayOfObjectLocator objLocs = getClientToHost().getData(arrayUUIDs, arrayTsUID, true);
		List<ObjectLocator> listObjLocs = objLocs.getObjectLocator();

		// Start a process with inputs array as command line arguments.
		// final String inputs = "/progra~1/Xip2DViewer/bin/RTViewer.exe " + getSceneGraphInput(listObjLocs);
		final String inputs = executableCommand + " " + executableArgs + " " + getSceneGraphInput(listObjLocs);
		System.out.println("Command Line: " + inputs);
		//TODO:  add exec here	
		try {
			String[] envp = null;
			//File wrkDir = new File("/progra~1/Xip2DViewer/config");
			File wrkDir = null; 
			if (executableDir != null) {
				wrkDir = new File(executableDir);
				
			}
			cmdProcess = Runtime.getRuntime().exec(inputs, envp, wrkDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void extractUUIDs (ArrayOfObjectDescriptor descriptors, List<UUID> listUUIDs, Set<UID> setTsUID) {
		if (descriptors == null)
			return;
		
		List<ObjectDescriptor> listDescriptors = descriptors.getObjectDescriptor();
		for(ObjectDescriptor desc : listDescriptors){
			listUUIDs.add(desc.getDescriptorUuid());
			setTsUID.add(desc.getTransferSyntaxUID());
		}
	}

	@Override
	public boolean setState(State newState) {
		appCurrentState = newState;
		if(State.valueOf(newState.toString()).equals(State.CANCELED)){
			getClientToHost().notifyStateChanged(State.CANCELED);
			getClientToHost().notifyStateChanged(State.IDLE);
		}else if(State.valueOf(newState.toString()).equals(State.EXIT)){
			getClientToHost().notifyStateChanged(State.EXIT);						
			//terminating endpoint and existing system is accomplished through ApplicationTerminator
			//and ApplicationScheduler. ApplicationSechduler is present to allow termination delay if needed (possible future use)
			ApplicationTerminator terminator = new ApplicationTerminator(getEndPoint());
			Thread t = new Thread(terminator);
			t.start();	
			cmdProcess.destroy();
		}else{
			getClientToHost().notifyStateChanged(newState);
		}
		return true;
	}
	
	@Override
	public State getState() {
		return appCurrentState;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void outputAvailable(OutputAvailableEvent e) {
		List<File> output = (List<File>)e.getSource();
		WG23DataModel wg23DM = new WG23DataModelImpl(output);		
		dataMgr.setOutputData(wg23DM);
		AvailableData availableData = wg23DM.getAvailableData();		
		getClientToHost().notifyDataAvailable(availableData, true);	
	}
}
