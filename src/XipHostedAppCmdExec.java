/*
Copyright (c) 2013, Washington University in St.Louis.
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.nema.dicom.wg23.ArrayOfObjectDescriptor;
import org.nema.dicom.wg23.ArrayOfObjectLocator;
import org.nema.dicom.wg23.ArrayOfUUID;
import org.nema.dicom.wg23.AvailableData;
import org.nema.dicom.wg23.ObjectDescriptor;
import org.nema.dicom.wg23.ObjectLocator;
import org.nema.dicom.wg23.Patient;
import org.nema.dicom.wg23.Series;
import org.nema.dicom.wg23.State;
import org.nema.dicom.wg23.Study;
import org.nema.dicom.wg23.Uuid;

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
				filePath = new File(objLocs.get(i).getUri()).getPath();
				// input = input + "\"" + nols.get(i).getURI() + "\"" + ", ";					
				filePath = filePath.substring(6 , filePath.length());
				input = filePath + " ";								
			} else if(i < size -1){
				String filePath = new File(objLocs.get(i).getUri()).getPath();
				//input = input + "\"" + nols.get(i).getURI() + "\"" + ", ";
				filePath = filePath.substring(6 , filePath.length());
				input = input + filePath + " ";
			}else if(i == size -1){
				String filePath = new File(objLocs.get(i).getUri()).getPath();
				//input = input + "\"" + nols.get(i).getURI() + "\"" + ", ";
				filePath = filePath.substring(6 , filePath.length());
				input = input + filePath;
			}				
		}
		return input;
	}
	
	@Override
	public boolean bringToFront() {
		// Schedule a job for the event-dispatching thread:
		// bringing to front.
		return true;
		//return false;
	}

	@Override
	public void notifyDataAvailable(AvailableData availableData,
			boolean lastData) {

		ArrayOfUUID arrayUUIDs = new ArrayOfUUID();
		List<Uuid> listUUIDs = arrayUUIDs.getUuid();

		// Extract UUIDs for all objects
		extractUUIDs (availableData.getObjectDescriptors(), listUUIDs);

		if ((availableData.getPatients() != null)
							&& (availableData.getPatients().getPatient() != null)) {
			List<Patient> patients = availableData.getPatients().getPatient();	
			for (Patient patient : patients) {
				if (patient == null) {
					continue;
				}
				extractUUIDs (patient.getObjectDescriptors(), listUUIDs);
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
					extractUUIDs (study.getObjectDescriptors(), listUUIDs);
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
						extractUUIDs (series.getObjectDescriptors(), listUUIDs);
					}
				}
			}
		}
		if (listUUIDs.isEmpty()) {
			return;
		}
		
		ArrayOfObjectLocator objLocs = getClientToHost().getDataAsFile(arrayUUIDs, true);
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
			Runtime.getRuntime().exec(inputs, envp, wrkDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void extractUUIDs (ArrayOfObjectDescriptor descriptors, List<Uuid> listUUIDs) {
		if (descriptors == null)
			return;
		
		List<ObjectDescriptor> listDescriptors = descriptors.getObjectDescriptor();
		for(ObjectDescriptor desc : listDescriptors){
			listUUIDs.add(desc.getUuid());
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
