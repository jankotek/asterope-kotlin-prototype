package skyview.executive;

/** This class transforms setting to enable compatibility
 *  between the old SkyView batch interface and the current Java application.
 */
public class BatchCompatibility implements skyview.executive.SettingsUpdater {
    
    /** Update the settings.
     */
    public void updateSettings() {
	
	if (Settings.has(Key.file) ) {
	    Settings.put(Key.output, Settings.get(Key.file));
        }
	
	if (Settings.has(Key.vcoord) ) {
	    Settings.put(Key.position, Settings.get(Key.vcoord));
	    // This is the mandatory keyword that anyone using
	    // the old interface would use.  We'll set the
	    // code to use 4-byte reals, but someone can
	    // say float=null to disable this.  This way
	    // people using the old interface get 4-byte reals
	    // as before.
	    Settings.suggest(Key.float_, "true");
	}
	
	if (Settings.has(Key.pixelx) && Settings.has(Key.pixely) ) {
	    Settings.put(Key.pixels, Settings.get(Key.pixelx)+","+
            Settings.get(Key.pixely));
	    
	    // Note the in the old version a scalar sfactr was the
	    // longer dimension and all pixels were square.
	    // We reset sfactr as a tuple if nx != ny
	    if (Settings.has(Key.SFACTR)  &&
		   Settings.get(Key.pixelx) != Settings.get(Key.pixely)) {
		String[] flds = Settings.getArray(Key.SFACTR);
		
		if (flds.length == 1) {
		    try {
			// Sfactr may have value 'default' which we
			// don't need to worry about.
			double size = -1;
			try {
			    size = Double.parseDouble(Settings.get(Key.SFACTR));
			} catch (Exception e) {
			    // Ignore it
			}
			if (size > 0) {
			    // Recover as double so we don't need to worry about truncation.
			    double nx   = Double.parseDouble(Settings.get(Key.pixelx));
			    double ny   = Double.parseDouble(Settings.get(Key.pixely));
			    if (nx > ny) {
			        Settings.put(Key.SFACTR, size + "," +ny/nx*size);
			    } else {
			        Settings.put(Key.SFACTR, nx/ny*size+","+size);
			    }
			}
		    } catch (Exception e) {
			// Proceed as best we can -- we should get errors later
			// on when the pixel values or size are read in.
		    }
		}
	    }
	}
	
	if ( Settings.has(Key.iscaln) ) {
	    String scal = Settings.get(Key.iscaln).toLowerCase();
	    if (scal.startsWith("lo")) {
		scal = "log";
	    } else if (scal.startsWith("li")) {
		scal = "linear";
	    } else if (scal.startsWith("hi")) {
		scal = "histeq";
	    } else {
		scal = "log";
	    }
	    Settings.put(Key.scaling, scal);
	}
	
	if (Settings.has(Key.imgree) && Settings.has(Key.imredd) &&
	    Settings.has(Key.imblue)) {
	    String surv = Settings.get(Key.imredd) + "," +
	                  Settings.get(Key.imgree) + "," +
	                  Settings.get(Key.imblue);
	    Settings.put(Key.survey, surv);
	    Settings.put(Key.rgb, "true");
	}
	
	if (Settings.has(Key.SFACTR) ) {
	    Settings.put(Key.size, Settings.get(Key.SFACTR));

	}
	
	if (Settings.has(Key.maproj)) {
	    String proj = Settings.get(Key.maproj).toLowerCase();
	    if (proj.startsWith("gnom")) {
		Settings.put(Key.projection, "Tan");
	    } else if (proj.startsWith("rect")) {
		Settings.put(Key.projection, "Car");
	    } else if (proj.startsWith("hamm")) {
		Settings.put(Key.projection, "Ait");
	    } else if (proj.startsWith("orth")) {
		Settings.put(Key.projection, "Sin");
	    } else if (proj.startsWith("zeni")) {
		Settings.put(Key.projection, "Zea");
	    } else {
		Settings.put(Key.projection, Settings.get(Key.maproj));
	    }
	}
	
	if (Settings.has(Key.resamp)) {
	    String samp = Settings.get(Key.resamp).toLowerCase();
	    if (samp.startsWith("interp")) {
		Settings.put(Key.sampler, "LI");
	    } else if (samp.startsWith("near")) {
		Settings.put(Key.sampler, "NN");
	    } else if (samp.startsWith("tri")) {
		Settings.put(Key.sampler, "Clip");
	    } else {
		Settings.put(Key.sampler, Settings.get(Key.resamp));
	    }
	}
	
	if (Settings.has(Key.griddd)) {
	    if (!Settings.get(Key.griddd).toLowerCase().equals("no")) {
	        if (!Settings.has(Key.grid)) {
		    Settings.put(Key.grid, "1");
		    Settings.put(Key.GridLabels, "1");
	        }
	    }
	}
	
	if (Settings.has(Key.CATLOG)) {
	    String[] catalogs = Settings.getArray(Key.CATLOG);
	    String newCat   = "";
	    String sep      = "";
	    for (int i=0; i<catalogs.length; i += 1) {
		newCat += sep + getCatalog(catalogs[i]); 
		sep     = ",";
	    }
	    Settings.put(Key.catalog, newCat);
	}
				       
		
	
	if (Settings.has(Key.equinx) ) {
	    Settings.put(Key.equinox, Settings.get(Key.equinx));
	}
	
	if (Settings.has(Key.scoord) ) {
            String csys = Settings.get(Key.scoord).toLowerCase();
	    if (csys.startsWith("equa")) {
	       Settings.put(Key.coordinates, "J");
	    } else {
	       Settings.put(Key.coordinates,
		 Settings.get(Key.scoord).substring(0,1));
	    }

	}
	
	if (Settings.has(Key.return_) && !Settings.has(Key.quicklook)) {
	    if (! Settings.get(Key.return_).toUpperCase().equals("FITS")) {
	        Settings.put(Key.quicklook, Settings.get(Key.return_));
		Settings.put(Key.nofits, "");
	    }
	}
	
	// This assume the LUT files are available in the JAR
	// (or that the user has put the colortables directly directly
	// beneath the execution directory.
	if (Settings.has(Key.coltab)) {
	    String lutFile = Settings.get(Key.coltab).trim();
	    lutFile = lutFile.replace(" ", "-");
	    lutFile = "colortables/"+lutFile+".bin";
	    Settings.put(Key.lut, lutFile);
	}
    }
    
        /** Catalog IDs as of March 27, 2006 */
	static String[] catIDs = new String[]{
"1.5-GHz VLA-NEP Survey","vlanep",
"20cm Radio Catalog","north20cm",
"3rd Ariel-V SSI Catalog","ariel3a",
"6cm Radio Catalog","north6cm",
"8th Orbital Elements Catalog","batten",
"ASCA GIS Source Catalog / ASCA Medium Sensitivity Survey","ascagis",
"ASCA Galactic Plane Survey of Faint X-Ray Sources","ascagps",
"ASCA Large Sky Survey","ascalss",
"ASCA Master Catalog","ascamaster",
"ASCA Proposals","ascao",
"ASCA SIS Source Catalog","ascasis",
"ATNF Pulsar Catalog","atnfpulsar",
"Abell Clusters","abell",
"Ap & Am Stars General Catalog","cpstars",
"Ariel V All-Sky Monitor","ariel5",
"Asiago Supernova Catalog 1999","asiagosn99",
"Astrographic Catalog of Reference Stars","acrs",
"BBXRT Archive","bbxrt",
"Be Stars Catalog","bestars",
"BeppoSAX 2-10 keV Survey","sax2to10",
"BeppoSAX Approved Pointings","saxao",
"BeppoSAX NFI Archive and Observation Log","saxnfilog",
"BeppoSAX WFC Observation Log","saxwfclog",
"Bootes Deep Field WSRT 1.4 GHz Source Catalog","bootesdf",
"Brera Multi-scale Wavelet ROSAT HRI Source Catalog","bmwhricat",
"Bright Star Catalog","bsc5p",
"CGRO BATSE-Observed Piccinotti Sample of Active Galactic Nuclei","cbatpicagn",
"CGRO Timeline","cgrotl",
"CGRO/BATSE 4B Catalog","batse4b",
"CGRO/BATSE Gamma-Ray Burst Catalog","batsegrb",
"CGRO/BATSE Occultations","batseocc",
"CGRO/BATSE Pulsar Observations","batsepulsr",
"CGRO/BATSE Trigger Data","batsetrigs",
"CGRO/COMPTEL Low-Level Data and Maps","comptel",
"CGRO/EGRET Photon Lists and Maps","egretdata",
"CGRO/EGRET Third Source Catalog","egret3",
"CGRO/OSSE Observations","osse",
"COS-B Map Product Catalog","cosbmaps",
"COS-B Photon Events Catalog","cosbraw",
"Candidate Galaxies Behind the Milky Way","cgmw",
"Cataclysmic Variables Catalog (Living Edition)","cvcat",
"Catalog CMA Central 6 Arcmin","le",
"Catalog of Galaxies Observed by the Einstein Observatory IPC & HRI","eingalcat",
"Catalog of Infrared Observations (CIO), Edition 5","infrared",
"CfA Redshift Catalog (June 1995 Version)","zcat",
"Chandra Deep Field North 1-Megasecond Catalog","chandfn1ms",
"Chandra Deep Field South 1-Megasecond Catalog","chandfs1ms",
"Chandra Observations","chanmaster",
"Chandra Public Observations","chandrapub",
"Chromospherically Active Binary Stars Catalog","cabscat",
"Copernicus X-Ray Observations","xcopraw",
"Crab Pulsar Timing","crabtime",
"Dixon Radio Sources","dixon",
"EINSTEIN IPC Unscreened Photon Event List","ipcunscrnd",
"ESO-Uppsala ESO(B) Survey","esouppsala",
"EUVE Archive and Observation Log","euvemaster",
"EUVE Bright Sources","euvebsl",
"EUVE Right Angle Program, 2nd Catalog","euverap2",
"EUVE Second Source Catalog","euvecat2",
"EXOSAT Bibliography","exopubs",
"EXOSAT CMA Images for Each Pointing","cmaimage",
"EXOSAT GSPC Spectra and Lightcurves","gs",
"EXOSAT ME Slew Catalog","exms",
"EXOSAT ME Spectra and Lightcurves","me",
"EXOSAT Master Observation List","exomaster",
"EXOSAT Observation Log","exolog",
"EXOSAT TGS L and R Orders","tgs",
"EXOSAT TGS Spectra and Lightcurves","tgs2",
"EXOSAT/CMA High Galactic Latitude Survey","exohgls",
"EXOSAT/CMA Sources","sc_cma_view",
"EXOSAT/ME Galactic Plane Survey","exogps",
"Einstein Catalog HRI CFA Sources","hricfa",
"Einstein Catalog HRI Deep Survey","hrideep",
"Einstein Catalog HRI ESTEC Sources","hriexo",
"Einstein Catalog IPC Deep Survey","ipcdeep",
"Einstein Catalog IPC EMSS Survey","emss",
"Einstein Catalog IPC Slew Survey","ipcslew",
"Einstein Count Rates for IPC O Stars","ipcostars",
"Einstein Extended Source Survey","exss",
"Einstein FPCS Events Files","fpcsfits",
"Einstein HRI Images","hriimage",
"Einstein HRI Photon Event Data","hriphot",
"Einstein IPC Images","ipcimage",
"Einstein IPC Photon Event Data","ipcphot",
"Einstein IPC Sources Catalog","ipc",
"Einstein IPC Ultrasoft Sources Catalog","ipcultsoft",
"Einstein LX & LBL Values for IPC O Stars","ipclxlbol",
"Einstein MPC Raw Data","mpcraw",
"Einstein Observation Log","einlog",
"Einstein Observatory 2E Catalog of IPC X-Ray Sources","einstein2e",
"Einstein SSS Spectra and Lightcurves","sss",
"Einstein SSS and MPC Raw Data","sssraw",
"Einstein Survey of Optically Selected Galaxies","einopslgal",
"Einstein Two-Sigma Catalog","twosigma",
"Extragalactic Radio Sources","kuehr",
"Faint Images of the Radio Sky at Twenty cm Source Catalog (FIRST)","first",
"Faust Far-UV Point Source Catalog","faust",
"First DENIS I-band Extragalactic Catalog","denisigal",
"Galactic Novae References Catalog","duerbeck",
"Galactic O Stars Catalog","ostars",
"Galactic Planetary Nebulae Catalog","plnebulae",
"Gamma-Ray Source Detailed Catalog (Macomb & Gehrels 1999)","mggammadet",
"Gamma-Ray Source Summary Catalog (Macomb & Gehrels 1999 & 2001)","mggammacat",
"General Catalog of Variable Stars","gcvs",
"General Catalog of Variable Stars: Extragalactic Supernovae","gcvsegsn",
"General Catalog of Variable Stars: Extragalactic Variables","gcvsegvars",
"General Catalog of Variable Stars: Suspected Variable Stars","gcvsnsvars",
"Ginga Background Lightcurves & Spectra","gingabgd",
"Ginga LAC Log Catalog","gingalog",
"Ginga LAC Mode Catalog","gingamode",
"Ginga LAC Raw Data","gingaraw",
"Ginga Source Lightcurves & Spectra","gingalac",
"Gliese Catalog of Nearby Stars, 3rd Edition","cns3",
"Green Catalog of Galactic SNRs (December 2001 Version)","snrgreen",
"GSC 2.2 Catalog (STScI, 2001)(VizieR)","I/271",
"HEAO 1 A1 Lightcurves","a1point",
"HEAO 1 A1 X-ray","a1",
"HEAO 1 A2 LED","a2led",
"HEAO 1 A2 Piccinotti","a2pic",
"HEAO 1 A2 Pointed Lightcurves","a2lcpoint",
"HEAO 1 A2 Pointing","a2point",
"HEAO 1 A2 Scanned Lightcurves","a2lcscan",
"HEAO 1 A2 Spectra","a2spectra",
"HEAO 1 A2 Spectra Background","a2specback",
"HEAO 1 A3 MC Lass","a3",
"HEAO 1 A4 Spectra","a4spectra",
"HEAO 1 A4 X-ray","a4",
"HETE-2 GCN Triggers Catalog","hete2gcn",
"HST Archived Exposures Catalog","hstaec",
"HST Planned and Archived Observations","hstpaec",
"Henry Draper Extension Charts Catalog","hdec",
"Herbig & Bell Catalog of Orion Pop. Emission-Line Stars","hbc",
"Hewitt & Burbidge (1991) Catalog of Extragalactic Emission-Line Objects","exgalemobj",
"Hewitt & Burbidge (1993) QSO Catalog","qso",
"Hickson Compact Groups of Galaxies (HCG) Catalog","hcg",
"Hicksons Compact Groups of Galaxies (HCG) Individual Galaxies Data","hcggalaxy",
"High-Mass X-Ray Binary Catalog (2000)","hmxbcat",
"Hipparcos Input Catalog","hic",
"Hipparcos Main Catalog","hipparcos",
"INTEGRAL Observing Program","integralao",
"IRAS 1.2-Jy Redshift Survey","iraszsurv",
"IRAS Faint Sources","irasfsc",
"IRAS Point Source Catalog, Version 2.0","iraspsc",
"ISO (Infrared Space Observatory) Observation Log of Validated Data","isolog",
"IUE (International Ultraviolet Explorer) Observation Log","iuelog",
"Kommers et al. (2001) BATSE Non-Triggered Gamma-Ray Burst Catalog","kommersgrb",
"LMC Clusters Catalog","lmcclustrs",
"LMC X-Ray Discrete Sources","lmcxray",
"Large Bright Quasar Survey","lbqs",
"Low-Mass X-Ray Binary Catalog (2001)","lmxbcat",
"Lynds Catalog of Bright Nebulae","lbn",
"Lynds Catalog of Dark Nebulae","ldn",
"Lynga Open Clusters Catalog","lyngaclust",
"M31 Field Brightest Stars Catalog","m31stars",
"M31 Globular Cluster Candidates Catalog","m31clustrs",
"MIT/Amsterdam M31 Survey","m31stars2",
"Magellanic Catalog of Stars","macs",
"Markarian Galaxies","markarian",
"Master EUV Catalog","euv",
"Master Optical Catalog","optical",
"Master Radio Catalog","radio",
"Master X-Ray Catalog","xray",
"McCook-Sion White Dwarf Catalog, 4th Edition (1999)","mcksion",
"Messier Nebulae","messier",
"Midcourse Space Experiment (MSX) Point Source Catalog, V1.2","msxpsc",
"Milky Way Globular Clusters Catalog (June 1999 Version)","globclust",
"Molonglo Radio Sources","mrc",
"Morphological Galaxy Catalog","mcg",
"NGC 2000.0 Catalog","ngc2000",
"NLTT Catalog & First Supplement","nltt",
"NRAO VLA Sky Survey Catalog","nvss",
"New Optically Visible Open Clusters & Candidates Catalog","openclust",
"OSO8 A Detector Lightcurves","oso8alc",
"OSO8 B&C Detector Lightcurves","oso8bclc",
"OSO8 GCXSE Raw Rates","oso8rtraw",
"PG Catalog of UV-excess Objects","pg",
"Palomar/MSU Nearby Star Survey","pmsucat",
"Parkes Multibeam Survey New Pulsar Catalog","pmpulsar",
"Parkes Southern Radio Sources","pkscat90",
"Parkes-MIT-NRAO (PMN) Surveys","pmn",
"Positions and Proper Motions Catalog","ppm",
"Pulsar Catalog","pulsar",
"RASS/Green Bank Catalog","rassgb",
"ROSAT All-Sky Survey Public Archival Data","rasspublic",
"ROSAT All-Sky Survey: A-K Dwarfs/Subgiants","rassdwarf",
"ROSAT All-Sky Survey: Bright Sources","rassbsc",
"ROSAT All-Sky Survey: Chamaeleon Star Forming Region Study","chasfrxray",
"ROSAT All-Sky Survey: Faint Sources","rassfsc",
"ROSAT All-Sky Survey: Giants & Supergiants","rassgiant",
"ROSAT All-Sky Survey: Hamburg Optical IDs","hrasscat",
"ROSAT All-Sky Survey: Hyades Cluster Region","hyadesxray",
"ROSAT All-Sky Survey: Nearby Stars","rasscns3",
"ROSAT All-Sky Survey: OB Stars","rassob",
"ROSAT All-Sky Survey: Soft High Galactic-Latitude X-Ray Sources","rasshgsoft",
"ROSAT All-Sky Survey: White Dwarves","rasswd",
"ROSAT Archival Data","rospublic",
"ROSAT Archival WFC EUV Data","wfcpoint",
"ROSAT Bright Survey (Schwope et al. 2000)","rbs",
"ROSAT Catalog PSPC WGA Sources","wgacat",
"ROSAT Catalog WFC 2RE Sources","roswfc2re",
"ROSAT Complete Results Archive Sources for the HRI","roshritotal",
"ROSAT Complete Results Archive Sources for the PSPC","rospspctotal",
"ROSAT Deep X-Ray Radio Blazar Survey","dxrbs",
"ROSAT HRI Catalog of LMC X-Ray Sources (Sasaki et al.)","lmchrixray",
"ROSAT HRI Orion Group 1 Stars","orionxstar",
"ROSAT Observation Log","rosatlog",
"ROSAT PSPC Catalog of Clusters of Galaxies","rosgalclus",
"ROSAT PSPC Catalog of LMC X-Ray Sources (Haberl & Pietsch)","lmcrosxray",
"ROSAT PSPC Catalog of SMC X-Ray Sources (Haberl et al)","smcrosxry2",
"ROSAT PSPC Catalog of the Pleiades (Micela et al. 1996)","pleiadxray",
"ROSAT PSPC M31 Source Catalog","m31rosxray",
"ROSAT PSPC Survey of the Small Magellanic Cloud","smcrosxray",
"ROSAT Radio-Loud Quasars Catalog","rosatrlq",
"ROSAT Radio-Quiet Quasars Catalog","rosatrqq",
"ROSAT Results Archive Sources for the HRI","roshri",
"ROSAT Results Archive Sources for the PSPC","rospspc",
"ROSAT Survey of the Orion Nebula","orionxray",
"ROSAT XUV Pointed Phase","rosatxuv",
"Revised Luyten Half-Second (LHS) Catalog","revisedlhs",
"Ritter Binaries Related to CVs Catalog","ritterrbin",
"Ritter Cataclysmic Binaries Catalog (6th Ed)","rittercv",
"Ritter Low-Mass X-Ray Binaries Catalog (6th Ed)","ritterlmxb",
"SAS-2 Map Product Catalog","sas2maps",
"SAS-2 Photon Events Catalog","sas2raw",
"SAS-3 Y-Axis Pointed Obs Log","sas3ylog",
"SMC & Bridge Clusters Catalog","smcclustrs",
"SMC H-Alpha Emission Stars/Nebulae","smcstars2",
"SMC Probable Member Stars Catalog","smcstars",
"SMC X-Ray Discrete Sources","smcxray",
"Shakbazian Compact Groups of Galaxies","shk",
"Shakhabazian (Shk) Compact Groups of Galaxies: Individual Galaxies Data","shkgalaxy",
"Sharpless H II Regions","hiiregion",
"Sloan Digital Sky Survey Quasar Catalog (Early Data Release)","sdssquasar",
"Smithsonian Astrophysical Observatory Star Catalog","sao",
"Stern et al. (2001) BATSE Gamma-Ray Burst Catalog","sterngrb",
"Sydney University Molonglo Sky Survey (SUMSS) Source Catalog","sumss",
"TD1 Stellar UV Fluxes","td1",
"TYCHO-2 Catalog of the 2.5 Million Brightest Stars","tycho2",
"Tartarus: Reduced ASCA AGN Data","tartarus",
"Texas Survey of Radio Sources at 365 MHz","texas",
"The VIIth Catalog of Galactic Wolf-Rayet Stars","wrcat",
"Third Reference Catalog of Galaxies","rc3",
"Uhuru Fourth (4U) Catalog","uhuru4",
"Ultraviolet Imaging Telescope Near-UV Bright Objects Catalog","uit",
"Updated Zwicky Catalog","uzc",
"Uppsala General Catalog of Galaxies","ugc",
"Vela 5B All-Sky Monitor Lightcurves","vela5b",
"Veron Quasars & AGNs (V2001)","veron2001",
"VizieR: The 2MASS Database (IPAC/UMass, 2000)","B/2mass",
"VizieR: USNO A2 Catalog (Monet)","I/252",
"WBL Individual Galaxies Data Catalog (White et al. 1999)","wblgalaxy",
"WBL Poor Galaxy Clusters Catalog (White et al. 1999)","wbl",
"Wackerling Catalog of Early-Type Emission-Line Stars","wackerling",
"Washington Double Star Catalog","wds",
"Westerbork Northern Sky Survey","wenss",
"Wood Interacting Binaries Catalog","woodebcat",
"Woolley Catalog of Stars within 25 Parsecs","woolley",
"X-Ray Binaries Catalog","xrbcat",
"XMM-Newton Accepted Targets","xmmao",
"XMM-Newton Master Log & Public Archive","xmmmaster",
"XMM-Newton Observation Log","xmmlog",
"XMM-Newton U.S. Public Archive","xmmpublic",
"XTE All-Sky Monitor Long-Term Observed Sources","xteasmlong",
"XTE All-Sky Monitor Quicklook Observed Data","xteasmquick",
"XTE Archived Public Data","xtepublic",
"XTE Archived Public Slew Data","xteslew",
"XTE Long-Term Schedule","xtelttlview",
"XTE Master Catalog","xtemaster",
"XTE Observation Log","xteobs",
"XTE Proposal Info & Abstracts","xteao",
"XTE Short-Term Schedule","xtesttlview",
"XTE Target Index Catalog","xteindex",
"Zwicky Clusters","zwclusters"};
    
    static java.util.HashMap<String,String> nameHash;
    
    /** Translate a catalog name to the form understood in the the Java application */
    private static String getCatalog(String oldCat) {
	
	if (nameHash == null) {
	    nameHash = new java.util.HashMap<String, String>();
	    for (int i=0; i<catIDs.length; i += 2) {
		nameHash.put(catIDs[i].toLowerCase(), catIDs[i+1]);
	    }
	}
	if (nameHash.containsKey(oldCat.toLowerCase())) {
	    return nameHash.get(oldCat.toLowerCase());
	} else {
	    return oldCat;
	}
    }
    
}
