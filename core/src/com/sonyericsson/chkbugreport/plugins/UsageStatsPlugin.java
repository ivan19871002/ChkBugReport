/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.util.DumpTree;
import com.sonyericsson.chkbugreport.util.Util;
import com.sonyericsson.chkbugreport.util.DumpTree.Node;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsageStatsPlugin extends Plugin {

    private static final String TAG = "[UsageStatsPlugin]";

    private boolean mLoaded;
    private Section mSection;
    private Vector<UsageHistory> mUsageHistory = new Vector<UsageHistory>();
    private int mNextUsageAnchor;

    @Override
    public int getPrio() {
        return 91;
    }

    @Override
    public void reset() {
        // Reset
        mLoaded = false;
        mSection = null;
        mNextUsageAnchor = 0;
    }

    @Override
    public void load(Module br) {
        // Load data
        mSection = br.findSection(Section.DUMP_OF_SERVICE_USAGESTATS);
        if (mSection == null) {
            br.printErr(3, TAG + "Section not found: " + Section.DUMP_OF_SERVICE_USAGESTATS + " (aborting plugin)");
            return;
        }
        // Parse the data
        DumpTree dump = new DumpTree(mSection, 0);

        for (DumpTree.Node Dateitem : dump) {
        	String line = Dateitem.getLine();
        	if (line.endsWith("(old data version)")) {
                br.printErr(3, TAG + "mLoaded = true :" + line); 
    	 	} else if (line.startsWith("Date:")) {
    	 		//parser Date
//    	 		br.printErr(3, TAG + "====" + line);
    	 		String fields[] = line.split(" ");
    	 		String Date = fields[1];
    	 		for (DumpTree.Node item : Dateitem) {
//    	 			br.printErr(3, TAG + "====++++" + item.getLine());
    	 			addUsageHistory(br, item, Date);
    	 		}
    	 	}
        }
        // Done
        mLoaded = true;
    }

    private void addUsageHistory(Module br, Node item, String Date) {
    	UsageHistory Uh = new UsageHistory();
    	Uh.anchor = new Anchor("a" + mNextUsageAnchor++);
        //com.android.launcher: 1 times, 63280 ms        
    	
	    Pattern p = Pattern.compile("(.*) ([0-9]+) times, ([0-9]+) ms");
	    Matcher m = p.matcher(item.getLine());
	    if (!m.matches()) {
	    	br.printErr(4, "Cannot parse alarm stat: " + item.getLine());
	        return;
	    }        	
	    Uh.pkgName = m.group(1);
	    Uh.mLaunchCount = Long.parseLong(m.group(2));
	    Uh.mUsageTime = Util.parseRelativeTimestamp(m.group(3));
	    Uh.Date = Date;
//	    br.printErr(3, TAG + "====++++****" + Uh.pkgName + " " + Uh.mLaunchCount + " times, " + Uh.mUsageTime + "ms");
        for (int i = 0; i < item.getChildCount(); i++) {
        	Node child = item.getChild(i);
	        //com.android.launcher2.Launcher: 1 starts, 1000-1500ms=1
	        Pattern p1 = Pattern.compile("(.*): ([0-9]+) starts, (.*)");
	        Pattern p2 = Pattern.compile("(.*): ([0-9]+) starts");
	        Matcher m1 = p1.matcher(child.getLine());
	        Matcher m2 = p2.matcher(child.getLine());
	        
	        if (!m1.matches() && !m2.matches()) {
	            br.printErr(4, "Cannot parse action stat: " + child.getLine());
	            return;
	        }   	          
	        
	        UsageAction action = new UsageAction();
	        if (m1.matches()) {
	        	action.mLaunch = m1.group(1);
	        	action.mLaunchTimes = Long.parseLong(m1.group(2));
	        	action.mCount = m1.group(3);
	        }
	        else if (m2.matches()) {
	        	action.mLaunch = m2.group(1);
	        	action.mLaunchTimes = Long.parseLong(m2.group(2));
	        	action.mCount = "NA";
	        }
	        	
            
	        Uh.actions.add(action);
        }
         
        mUsageHistory.add(Uh);
    }

    @Override
    public void generate(Module rep) {
        if (!mLoaded) return;
        BugReportModule br = (BugReportModule) rep;

        // Generate the report
        Chapter mainCh = new Chapter(br.getContext(), "UsageHistory");
        br.addChapter(mainCh);

        genUsageHistory(br, mainCh);
        genUsageHistoryDetailed(br, mainCh);
    }
    

    private Chapter genUsageHistory(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "UsageHistory List");
        mainCh.addChapter(ch);
        
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, "UsageHistory");
        tg.setTableName(br, "UsageHistory");
        //com.zdworks.android.zdclock: 0 times, 26289 ms
        tg.addColumn("Pkg", null, Table.FLAG_NONE, "pkg");
        tg.addColumn("LaunchCount", null, Table.FLAG_ALIGN_RIGHT, "LaunchCount int");      
        tg.addColumn("UsageTime(ms)", null, Table.FLAG_SORT, "UsageTime_ms int");
//        tg.addColumn("UsageTime(ms)", null, Table.FLAG_ALIGN_RIGHT, "UsageTime_ms int");
        tg.addColumn("UsageTime", null, Table.FLAG_ALIGN_RIGHT, "UsageTime varchar");
        tg.addColumn("Date", null, Table.FLAG_NONE, "date");
        tg.begin();

        for (UsageHistory Uh : mUsageHistory) {
            tg.addData(new Link(Uh.anchor, Uh.pkgName));
            tg.addData(new ShadedValue(Uh.mLaunchCount));
            tg.addData(new ShadedValue(Uh.mUsageTime));
            tg.addData(Util.formatTS(Uh.mUsageTime));
//            tg.addData(formatter.format(Uh.mUsageTime - TimeZone.getDefault().getRawOffset()));
            tg.addData(Uh.Date);
        }
        tg.end();
        return ch;
    }

    private Chapter genUsageHistoryDetailed(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "UsageHistory detailed");
        mainCh.addChapter(ch);

        for (UsageHistory stat : mUsageHistory) {
            Chapter childCh = new Chapter(br.getContext(), stat.pkgName);
            ch.addChapter(childCh);
            //com.tencent.mm.plugin.sns.ui.SnsTimeLineUI: 4 starts, 750-1000ms=1
            childCh.add(stat.anchor);
            Table tg = new Table(Table.FLAG_SORT, childCh);
            tg.setCSVOutput(br, "UsageHistory_detailed_" + stat.pkgName);
            tg.addColumn("Action", Table.FLAG_NONE);
            tg.addColumn("LaunchTimes", Table.FLAG_ALIGN_RIGHT);
            tg.addColumn("Count", Table.FLAG_NONE);          
            tg.begin();

            for (UsageAction act : stat.actions) {
            	tg.addData(act.mLaunch);
                tg.addData(new ShadedValue(act.mLaunchTimes));
                tg.addData(act.mCount);
            }
            tg.end();
        }
        return ch;
    }

    class UsageAction {
        public String mLaunch;
        public long mLaunchTimes;
        public String mCount;
    }
    
    class UsageHistory {
        public String pkgName;
        public long mLaunchCount;
        public long mUsageTime;
        public String Date;
        public Vector<UsageAction> actions = new Vector<UsageAction>();
        public Anchor anchor;
    }
}
