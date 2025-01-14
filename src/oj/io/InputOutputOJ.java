/*
 * InputOutputOJ.java
 * -- documented
 *
 * methods to load and save project and results
 */
package oj.io;

import java.util.logging.Level;
import java.util.logging.Logger;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.frame.Editor;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import oj.OJ;
import oj.gui.menuactions.ProjectActionsOJ;
import oj.gui.results.ProjectResultsOJ;
import oj.gui.settings.ProjectSettingsOJ;
import oj.util.UtilsOJ;
import oj.project.*;
import oj.io.spi.IIOProviderOJ;
import oj.io.spi.IOFactoryOJ;
import oj.macros.EmbeddedMacrosOJ;
import oj.project.results.ColumnsOJ;
import static oj.util.UtilsOJ.showInFinderOrExplorer;

public class InputOutputOJ {

	private static String currentDirectory = "";

	/**
	 * @return directory containing the ojj file
	 */
	public static String getCurrentDirectory() {
		return currentDirectory;
	}

	/**
	 * directory containing the ojj file
	 */
	public static void setCurrentDirectory(String directory) {
		currentDirectory = directory;
	}

	/**
	 * Save embedded macros with project name and txt in project folder and
	 * "double-click" it there
	 */
	public static void exportEmbeddedMacros() {//23-12-2017
		String text = OJ.getData().getLinkedMacroText();
		Editor ed = OJ.editor;
		if (ed != null) {
			text = ed.getText();
		}
		String txtName = OJ.getData().getName() + ".txt";
		String path = getCurrentDirectory() + txtName;
		File f = new File(path);
		if (f.exists() && !f.canWrite()) {
			IJ.showMessage("Editor", "Unable to save because file is write-protected. \n \n" + path);
			return;
		}
		char[] chars = new char[text.length()];
		text.getChars(0, text.length(), chars, 0);
		try {
			BufferedReader br = new BufferedReader(new CharArrayReader(chars));
			BufferedWriter bw = new BufferedWriter(new FileWriter(path));
			while (true) {
				String s = br.readLine();
				if (s == null) {
					break;
				}
				bw.write(s, 0, s.length());
				bw.newLine();
			}
			bw.close();
			IJ.showStatus(text.length() + " chars saved to " + path);

			showInFinderOrExplorer(OJ.getData().getDirectory(), txtName);

			//IJ.runMacro("exec('open', getArgument())", path);
		} catch (IOException e) {
		}
	}

	static String ago(File file) {
		String date = "";
		if (!file.exists()) {
			return date;
		}
		long now = new Date().getTime();
		long modified = file.lastModified();
		long mSecs = now - modified;
		double minutes = ((double) (mSecs)) / 1000 / 60;
		String ago = "  (" + IJ.d2s(minutes, 1) + " minutes ago)";
		if (minutes > 99) {
			ago = "  (" + IJ.d2s(minutes / 60, 1) + " hours ago)";
		}
		if (minutes > 48 * 60) {
			ago = "  (" + IJ.d2s(minutes / 60 / 24, 0) + " days ago)";
		}
		date += (new Date(file.lastModified())).toString() + ago;
		return date;
	}

	/**
	 * Load text file (project name + .txt from project folder and install it as
	 * embedded macros.
	 */
	public static void replaceEmbeddedMacros(boolean withAltKey) {//23-12-2017

		String dir = getCurrentDirectory();
		String fName = OJ.getData().getName() + ".txt";
		String xPath = dir + fName;//exchangePath
		File xFile = new File(xPath);//exchangeFile
		String newMacroText = "";
		boolean xExists = xFile.exists();
		boolean fromOjj = false;
		boolean fromTxt = false;

		if (withAltKey) {
			if (!xExists) {
				IJ.showMessage("Alt key was down, but 'Exchange file' does not exist");
				return;
			}
			newMacroText = IJ.openAsString(xPath);
		}

		if (!withAltKey) {
			String multiLinePath = xPath;
			if (multiLinePath.length() > 80) {
				multiLinePath = multiLinePath.replace("/", "/\n");
			}
			if (!withAltKey) {
				int index = 0;
				Color color = Color.decode("#aa0000");
				if (xExists) {
					index = 1;
					color = Color.decode("#008800");

				}
				GenericDialog gd = new GenericDialog("Replace embedded macro text");
				String[] radios = ("From any .txt or .ojj file;From 'Exchange File' *").split(";");
				gd.addRadioButtonGroup("\nReplace and install embedded macros :", radios, 2, 1, radios[index]);
				int sizeEmbeddedMacros = OJ.getData().getLinkedMacroText().length();

				String msg2 = "";
				if (xExists) {
					msg2 += "\n *'Exchange File' exists:\n";

				} else {
					msg2 += "\n *'Exchange File' does not exist:";
				}
				msg2 += "\n " + multiLinePath;

				if (xExists) {
					msg2 += "\n \nModified:  " + ago(xFile);
					msg2 += "\nSize (bytes): " + xFile.length();;

				}

				gd.addMessage(msg2, Font.decode("Arial-12"), color);

				gd.addMessage("\nEmbedded macros:\nSize (bytes): " + sizeEmbeddedMacros, Font.decode("Arial-12"));

				String msg4 = "\n \n* Note: ";
				msg4 += "\n 'Exchange File' has same name and location as ";
				msg4 += "\n the .ojj project file, but extension is .txt instead .ojj ";
				msg4 += "\n \n It simplifies the installation of embedded macros that were ";
				msg4 += "\n edited elsewhere, but has no function otherwise";
				msg4 += "\n \n This dialog is skipped when the Alt key was down";
				gd.addMessage(msg4, Font.decode("Arial-12"));
				gd.showDialog();
				if (gd.wasCanceled()) {
					return;
				}
				String radioStr = gd.getNextRadioButton();
				if (radioStr.equals(radios[0])) {

					boolean savedFC = Prefs.useJFileChooser;
					if(IJ.isMacOSX())
						Prefs.useJFileChooser = true;
					String fromPath = IJ.getFilePath("Choose .txt or .ojj file to replace embedded macros");
					Prefs.useJFileChooser = savedFC;//15.10.2019
					if(fromPath == null){
						IJ.showStatus("Nothing was imported");
						return;
					}
					int separatorIndex = fromPath.lastIndexOf(File.separator);
					int len = fromPath.length();
					String name = fromPath.substring(separatorIndex + 1, len);
					String projectDir = fromPath.substring(0, separatorIndex);

					if (fromPath.toLowerCase().endsWith(".txt")) {
						xPath = fromPath;
						fromTxt = true;
						newMacroText = IJ.openAsString(xPath);

					} else if (fromPath.toLowerCase().endsWith(".ojj")) {
						newMacroText = extractEmbeddedMacros(projectDir, name);
						fromOjj = true;
					} else {
						IJ.showMessage("Extension must be  '.txt' or '.ojj' :   \nYou chose:\n \n" + fromPath);
					}

				}
				if (radioStr.equals(radios[1])) {
					newMacroText = IJ.openAsString(xPath);
				}
			}
		}

		if (!xExists && !fromOjj && !fromTxt) {
			IJ.showMessage("Cancelled");
			return;
		}

		OJ.getData().setLinkedMacroText(newMacroText);
		EmbeddedMacrosOJ.getInstance().doInstall(newMacroText);
		Editor ed = OJ.editor;
		if (ed != null) {
			ed.getTextArea().setText(newMacroText);
			ed.getTextArea().setSelectionEnd(0);//13.10.2019
			ed.getTextArea().setSelectionStart(0);
			ed.getTextArea().setCaretPosition(0);
			EmbeddedMacrosOJ.getInstance().setEditorUnchanged(ed);//29.3.2021
		}
		int nMacros = OJ.getData().getMacroSet().getMacrosCount();
		IJ.selectWindow("ImageJ");
		IJ.showStatus("*** Installed " + nMacros + " Embedded Macros");
	}
//   

//usually there is already a project file that now needs to be overwritten.
//If old project file does not exist, then a warning is issued:
//e.g. the old project file may have been moved or trashed in the Finder.
//now the user can save it under this or a different name.
	/**
	 * saves the project in an .ojj file. old version: xml text is saved
	 * uncompressed new version: binary and macro are put in a compressed zip
	 * archive
	 *
	 * @return true if successful
	 */
	public boolean saveProject(DataOJ data, boolean itsBinary) {
		ij.IJ.showStatus("Saving ObjectJ project...");
		if ((data.getDirectory() != null) && (data.getFilename() != null) && (new File(data.getDirectory(), data.getFilename()).exists())) {
			OJ.getImageProcessor().updateImagesProperties();//verify scaling and stackdimensions when opening and saving a project 16.5.2020
			File f = new File(data.getDirectory(), data.getName() + FileFilterOJ.objectJFileFilter().getExtension());

			if (!oj.OJ.loadedAsBinary) {
				boolean flag = ij.IJ.showMessageWithCancel("Saving Project", "Project file was loaded in XML format, and will now be saved in the newer Binary format.");
				if (!flag) {
					return false;
				}
				oj.OJ.loadedAsBinary = true;
			}
			if (f.exists()&& f.canWrite()) {
				boolean useBinary = OJ.saveAsBinary;//10.4.2010
				return saveProject(data, data.getDirectory(), data.getFilename(), useBinary);
			} else {
			    
			    if(f.exists() && !f.canWrite())
				ij.IJ.showMessage("Cannot overwrite project file");
				boolean pathMayChange = false;
				return saveProjectAs(data, itsBinary, pathMayChange);//, true
			}
		} else {
			boolean pathMayChange = true;
			IJ.showMessage("Couldn't relocate project file on disk");
			return saveProjectAs(data, itsBinary, pathMayChange);//, true
		}
	}

	
		public boolean saveProjectNew(DataOJ data, boolean itsBinary) {
		ij.IJ.showStatus("Saving ObjectJ project...");
		if ((data.getDirectory() != null) && (data.getFilename() != null) && (new File(data.getDirectory(), data.getFilename()).exists())) {
			File f = new File(data.getDirectory(), data.getName() + FileFilterOJ.objectJFileFilter().getExtension());

			if (!oj.OJ.loadedAsBinary) {
				boolean flag = ij.IJ.showMessageWithCancel("Saving Project", "Project file was loaded in XML format, and will now be saved in the newer Binary format.");
				if (!flag) {
					return false;
				}
				oj.OJ.loadedAsBinary = true;
			}
			if (f.exists() && f.canWrite()) {
				boolean useBinary = true;
				return saveProject(data, data.getDirectory(), data.getFilename(), useBinary);
			} else {
			    if(ij.IJ.showMessageWithCancel("Error","Cannot save project file here, save elsewhere?")){
				boolean pathMayChange = false;
				return saveProjectAs(data, itsBinary, pathMayChange);//, true
			    }
			    return false;
			}
		} else {
			boolean pathMayChange = true;
			IJ.showMessage("Couldn't relocate project file on disk");
			return saveProjectAs(data, itsBinary, pathMayChange);//, true
		}
	}

	/**
	 * Saves a copy of current project for backup
	 */
	public boolean saveACopy(DataOJ data) {
		boolean ok = true;
		String tmpName = data.getName();
		String tmpDir = data.getDirectory();
		String tmpFileName = data.getFilename();
		String fName = data.getName();
		if (fName.length() < 20) {
			fName += "-Copy";
		}
		fName += ".ojj";
		String fNameNoExtension = "";
		String dir = "";
		SaveDialogOJ sd = new SaveDialogOJ("Save a copy of project ...", fName, FileFilterOJ.objectJFileFilter());
		if (sd.isApproved()) {
			try {
				fName = sd.getFilename();
				dir = sd.getDirectory();
				int index = fName.lastIndexOf(".");
				fNameNoExtension = fName;
				if (index > 0) {
					fNameNoExtension = fName.substring(0, index);
				}

			} catch (Exception e) {
				IJ.error("error 5456");
				ok = false;
			}
			if (ok) {
				try {
					data.setName(fNameNoExtension);
					IIOProviderOJ ioProvider = IOFactoryOJ.getFactory().getProvider("javaobject");
					ioProvider.saveProject(data, dir, fName);

				} catch (Exception e) {
					IJ.error("error 9843");
					ok = false;
				}
			}
		}
		//data.setImages(tmpImages);
		//data.setCells(tmpCells);
		data.setName(tmpName);
		data.setDirectory(tmpDir);
		data.setFilename(tmpFileName);
		return ok;
	}

	/**
	 * Sets Images and Cells temporarily to zero and saves an empty copy, so it
	 * can be used for a similar experiment. All other information is retained:
	 * macros, object defs, column defs. Caution: unlinked columns are not
	 * emptied.
	 */
	public boolean saveEmptyProject(DataOJ data) {
		boolean ok = true;
		CellsOJ tmpCells = data.getCells();
		ImagesOJ tmpImages = data.getImages();
		String tmpName = data.getName();
		String tmpDir = data.getDirectory();
		String tmpFileName = data.getFilename();
		data.setCells(new CellsOJ());
		data.setImages(new ImagesOJ());
		String fName = "Untitled.ojj";
		String fNameNoExtension = "";
		String dir = "";
		SaveDialogOJ sd = new SaveDialogOJ("Save empty copy ...", fName, FileFilterOJ.objectJFileFilter());
		if (sd.isApproved()) {
			try {
				fName = sd.getFilename();
				dir = sd.getDirectory();
				int index = fName.lastIndexOf(".");
				fNameNoExtension = fName;
				if (index > 0) {
					fNameNoExtension = fName.substring(0, index);
				}

			} catch (Exception e) {
				IJ.error("error 9895");
				ok = false;
			}
			if (ok) {
				try {
					data.setName(fNameNoExtension);
					IIOProviderOJ ioProvider = IOFactoryOJ.getFactory().getProvider("javaobject");
					ioProvider.saveProject(data, dir, fName);

				} catch (Exception e) {
					IJ.error("error 8871");
					ok = false;
				}
			}
		}
		data.setImages(tmpImages);
		data.setCells(tmpCells);
		data.setName(tmpName);
		data.setDirectory(tmpDir);
		data.setFilename(tmpFileName);
		return ok;
	}

	/**
	 * will only be used in the new zipped version which contains the macro
	 */
	//public boolean saveProjectAs(DataOJ data, boolean itsBinary, boolean saveACopy/*, boolean withContent*/) {
	public boolean saveProjectAs(DataOJ data, boolean itsBinary, boolean pathMayChange) {

		boolean ok = false;

		if (data != null) {
			String oldDir = data.getDirectory();
			String oldFileName = data.getFilename();
			String oldProjectName = data.getName();

			String newDir = "";
			String newFileName = "";
			String newProjectName = "";

			String defaultName = data.getName();
			if ((data.getFilename() != null) && (!data.getFilename().equals(""))) {
				int index = data.getFilename().lastIndexOf(".");
				if (index > 0) {
					defaultName = data.getFilename().substring(0, index);
				}
			} else {
				defaultName = "Untitled";
			}
			SaveDialogOJ sd;
			if (itsBinary) {
				sd = new SaveDialogOJ("Save project ...", defaultName, FileFilterOJ.objectJFileFilter());
			} else {
				sd = new SaveDialogOJ("Save project ...", defaultName, FileFilterOJ.xmlFileFilter());
			}
			if (sd.isApproved()) {
				try {
					newFileName = sd.getFilename();
					newDir = sd.getDirectory();
					int index = sd.getFilename().lastIndexOf(".");
					if (index > 0) {
						newProjectName = sd.getFilename().substring(0, index);
					}

					data.setName(newProjectName);
					ok = saveProject(data, sd.getDirectory(), sd.getFilename(), itsBinary);//30.9.2010

				} catch (Exception e) {
					IJ.error("OJ_Prefs.txt contains binary=true?");
					ok = false;
				}
			} else {
				ok = false;
			}

			if (!pathMayChange) {
				data.setDirectory(oldDir);
				data.setFilename(oldFileName);
				data.setName(oldProjectName);
			} else {
				data.setDirectory(newDir);
				data.setFilename(newFileName);
				data.setName(newProjectName);
			}
		}
		OJ.isProjectOpen = true;//4.10.2010
		return ok;

	}

	/**
	 * saves current project under same name. depending on flag "asBinary",
	 * selects either "xmlstream" or "javaobject" provider, then calls that
	 * provider's "saveProject()" method. Before saving, killBadCells() removes
	 * cells with zero ytems, and ytems with zero points
	 *
	 * @return true if successful
	 */
	private boolean saveProject(DataOJ data, String directory, String filename, boolean asBinary) {
		//asBinary = false;
		int error = 6004;
		if (data != null) {
			data.getCells().killBadCells();

			data.setDescription(data.xmlComment);

		}
		try {
			data.setFilename(filename);
			data.setDirectory(directory);
			error = 6005;
			data.getResults().recalculate();
			error = 6006;
			data.updateChangeDate();
			if (asBinary) {
				IIOProviderOJ ioProvider = IOFactoryOJ.getFactory().getProvider("javaobject");
				ioProvider.saveProject(data, directory, filename);
				return true;
			} else {
				error = 6007;
				IIOProviderOJ ioProvider = IOFactoryOJ.getFactory().getProvider("xmlstream");
				error = 6008;
				ioProvider.saveProject(data, directory, filename);
				return true;
			}
		} catch (Exception e) {
			IJ.error("error = " + error + ":  " + e.getMessage());
			return false;
		}
	}

	/**
	 * Called by ResultActionsOJ to save results as text
	 */
	public void saveResultsAsText(String txt, String defaultName) {
		DataOJ data = oj.OJ.getData();
		if (data != null) {
			OpenDialog.setDefaultDirectory(data.getDirectory());//26.8.2010
		}
		SaveDialogOJ sd = new SaveDialogOJ("Save results ...", defaultName, FileFilterOJ.objectResultTextFileFilter());
		if (sd.isApproved()) {
			try {
				FileWriter fos = new FileWriter(new File(sd.getDirectory(), sd.getFilename()));
				PrintWriter out = new PrintWriter(fos);
				out.println(txt);
				out.close();
			} catch (Exception e) {
				IJ.error("error 3365: " + e.getMessage());
			}

		}
	}

	/**
	 * Asks to load a project
	 */
	public DataOJ loadProjectWithDialog() {
		OpenDialogOJ od = new OpenDialogOJ("Open ObjectJ project ...", FileFilterOJ.objectJFileFilter());
		if (od.isApproved()) {
			return loadAProject(od.getDirectory(), od.getFilename());
		} else {
			return null;
		}

	}

	public static String extractEmbeddedMacros(String directory, String filename) {
		String macros = null;
		String theType = UtilsOJ.getFileType(directory, filename);
		try {
			if (theType.startsWith("isZipped")) {
				IIOProviderOJ ioProvider = IOFactoryOJ.getFactory().getProvider("javaobject");
				macros = ioProvider.extractMacros(directory, filename);
			}
		} catch (ProjectIOExceptionOJ ex) {
			Logger.getLogger(InputOutputOJ.class.getName()).log(Level.SEVERE, null, ex);
			IJ.showMessage(ex.getMessage());
			return null;
		}
		return macros;
	}

	/**
	 * Depending on file type, selects either the "xmlstream" or the "javaobject
	 * provider, then calls that provider's "loadProject" method.
	 *
	 * @return dataOJ or null
	 */
	public DataOJ loadAProject(String directory, String filename) {
		
		ProjectSettingsOJ.close();//23-3-2020
		DataOJ dataOj = null;
		OJ.editor = null;
		ProjectResultsOJ.kill();//18.11.2018
		//OJ.doubleBuffered = false; //10.2.2011
		String theType = UtilsOJ.getFileType(directory, filename);
		try {
			if (theType.startsWith("isZipped")) {
				IIOProviderOJ ioProvider = IOFactoryOJ.getFactory().getProvider("javaobject");
				dataOj = ioProvider.loadProject(directory, filename);
				oj.OJ.loadedAsBinary = true;
				OJ.isProjectOpen = true;
			} else {
				IIOProviderOJ ioProvider = IOFactoryOJ.getFactory().getProvider("xmlstream");
				if (ioProvider.isValidData(directory, filename)) {
					IJ.showStatus("Please wait while loading project file ...");
					dataOj = ioProvider.loadProject(directory, filename);
					oj.OJ.loadedAsBinary = false;
					OJ.isProjectOpen = true;
				}
			}
		} catch (ProjectIOExceptionOJ ex) {
			Logger.getLogger(InputOutputOJ.class.getName()).log(Level.SEVERE, null, ex);
			IJ.showMessage(ex.getMessage());
			return null;
		}

		if (dataOj != null) {//repair if z=0
			int nCells = dataOj.getCells().getCellsCount();
			int hits = 0;
			boolean repairFlag = false;
			for (int pass = 1; pass <= 2; pass++) {
				for (int jj = 0; jj < nCells; jj++) {
					CellOJ cell = dataOj.getCells().getCellByIndex(jj);
					int nYtems = cell.getYtemsCount();
					for (int ytm = 0; ytm < nYtems; ytm++) {
						YtemOJ ytem = cell.getYtemByIndex(ytm);
						int slice = ytem.getStackIndex();
						for (int ll = 0; ll < ytem.getLocationsCount(); ll++) {
							double z = ytem.getLocation(ll).z;
							if (z == 0.0) {
								hits++;
								if (repairFlag) {
									ytem.getLocation(ll).z = slice;
								}
							}
						}
					}
				}
				if (pass == 1 && hits > 0) {
					repairFlag = ij.IJ.showMessageWithCancel("", "Found coordinates z==0; Repair?\n (you should click \"OK\")");
				}
			}
		}
			
		try {
			dataOj.getResults().getColumns().fixColumnsOrder();//2.2.2014

		} catch (Exception e) {
			IJ.showMessage("Exception 1878: " + e.toString());
		}
		ColumnsOJ columns = dataOj.getResults().getColumns();
		String titles[] = columns.columnNamesToArray();
		int max = titles.length;
		int nUnlinked = 0;
			for (int jj = max - 1; jj >= 0; jj--) {
			if (titles[jj].startsWith("_")){
				columns.removeColumnByIndex(jj);
				nUnlinked++;
				if(nUnlinked == 1){
					IJ.log("\\Clear");
					IJ.log("--- Unlinked columns: ---");
				}
				IJ.log(titles[jj]);
			}	
			
		}
		if(nUnlinked > 0){
		    //IJ.runMacro("waitForUser;");
		    
			String msg = "This project file contains unlinked results";
			msg +="\nwhich are not supported after ObjectJ version 1.04w";
			
			msg += "\n \nFor keeping this project file untouched,";
			msg +="\nclick 'OK' and choose  'ObjectJ>Project>Close Project'";
			msg +="\n \nOtherwise continue without unlinked results";
			
			String version = "ObjectJ " + OJ.releaseVersion;
			IJ.showMessage(version, msg);
		}
		dataOj.setChanged(false);
                
                //KB added pref here to retain the last opened project for next time
                Prefs.set("lastopened.directory", directory);
                Prefs.set("lastopened.filename", filename);
                
		return dataOj;
	}

            
        /**
         * Loads the last opened project. Asks to load a project if there is a problem.
         */
        public DataOJ loadProjectLastOpened() {
            //KB added pref here to retain the last opened project for next time
            String directory=Prefs.get("lastopened.directory", "");
            String filename=Prefs.get("lastopened.filename", "");
            Prefs.savePreferences();
            if(directory == "" || filename==""){
                    return loadProjectWithDialog();
            }else{	
                    return loadAProject(directory, filename);
            }
        }
        
   
	/**
	 *
	 * Special exception object for project IO doesn't do anything special
	 */
	public static class ProjectIOExceptionOJ extends Exception {

		public ProjectIOExceptionOJ(String message) {
			super(message);
		}
	}

}
