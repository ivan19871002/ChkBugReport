/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.charteditor;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartPluginInfo;
import com.sonyericsson.chkbugreport.chart.ChartPluginRepo;
import com.sonyericsson.chkbugreport.util.SaveFile;
import com.sonyericsson.chkbugreport.util.SavedData;
import com.sonyericsson.chkbugreport.util.Util;
import com.sonyericsson.chkbugreport.webserver.JSON;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

public class Charts extends SavedData<ChartData> {

    public Charts(SaveFile saveFile) {
        super(saveFile, "charts");
        load();
    }

    public ChartData find(String chartName) {
        if (chartName != null) {
            for (ChartData chart : getData()) {
                if (chartName.equals(chart.getName())) {
                    return chart;
                }
            }
        }
        return null;
    }

    @Override
    protected ChartData createItem() {
        return new ChartData("");
    }

    public void listPlugins(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        JSON plugins = json.addArray("plugins");
        ChartPluginRepo repo = mod.getChartPluginRepo();
        for (ChartPluginInfo info : repo) {
            plugins.add(info.getName());
        }
        json.writeTo(resp);
    }

    public void listCharts(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        JSON charts = json.addArray("charts");
        for (ChartData chart : getData()) {
            charts.add(chart.getName());
        }
        json.writeTo(resp);
    }

    public void newChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        if (name == null || name.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Name is not specified or empty!");
        } else if (!name.matches("[a-zA-Z0-9_]+")) {
            json.add("err", 400);
            json.add("msg", "Invalid characters in name!");
        } else if (null != find(name)) {
            json.add("err", 400);
            json.add("msg", "A chart with that name already exists!");
        } else {
            ChartData chart = new ChartData(name);
            add(chart);
            json.add("err", 200);
            json.add("msg", "Chart created!");
        }
        json.writeTo(resp);
    }

    public void getChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        ChartData chart = find(name);
        if (name == null || name.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Name is not specified or empty!");
        } else if (chart == null) {
            json.add("err", 400);
            json.add("msg", "A chart with that name does not exists!");
        } else {
            json.add("err", 200);
            json.add("id", chart.getId());
            json.add("name", chart.getName());
            JSON plugins = json.addArray("plugins");
            for (String plugin : chart.getPluginsAsArray()) {
                plugins.add(plugin);
            }
        }
        json.writeTo(resp);
    }

    public void deleteChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        ChartData chart = find(name);
        if (chart == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find chart!");
        } else {
            delete(chart);
            json.add("err", 200);
            json.add("msg", "Chart deleted!");
        }
        json.writeTo(resp);
    }

    public void deleteChartPlugin(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        String plugin = req.getArg("plugin");
        ChartData chart = find(name);
        if (chart == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find chart!");
        } else {
            chart.deletePlugin(plugin);
            update(chart);
            json.add("err", 200);
            json.add("msg", "Chart updated!");
        }
        json.writeTo(resp);
    }

    public void addChartPlugin(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        String plugin = req.getArg("plugin");
        ChartData chart = find(name);
        if (plugin == null || plugin.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Plugin is not specified or empty!");
        } else if (chart == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find chart!");
        } else {
            chart.addPlugin(plugin);
            update(chart);
            json.add("err", 200);
            json.add("msg", "Chart updated!");
        }
        json.writeTo(resp);
    }

}
