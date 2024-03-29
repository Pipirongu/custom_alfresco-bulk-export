/**
 *  This file is part of Alfresco Bulk Export Tool.
 * 
 *  Alfresco Bulk Export Tool is free software: you can redistribute it 
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the 
 *  License, or (at your option) any later version.
 *
 *  Alfresco Bulk Export Tool  is distributed in the hope that it will be 
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along 
 *  with Alfresco Bulk Export Tool. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.extensions.bulkexport.model;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.WebScriptResponse;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;


/**
 * This class manage the files and folders creation
 * 
 * @author Denys G. Santos (gsdenys@gmail.com)
 * @version 1.0.1
 */
public class FileFolder 
{
    Log log = LogFactory.getLog(FileFolder.class);

    /** {@link String} interface to web page for displaying messages
     *  server
     */
    private WebScriptResponse ui;
 
    /** {@link String} path to export data location in Alfresco
     *  server
     */
    private String basePath;
    
    /** {@link Boolean} value to avaliate if ovewrite content 
     * exported or no 
     */
    private boolean scapeExported;
    
    /**
     * File Folder default builder
     * 
     * @param basePath
     */
    public FileFolder(WebScriptResponse ui, String basePath, boolean scapeExported) 
    {
        log.debug("debug enabled for FileFolder");
        this.basePath = basePath;
        this.scapeExported = scapeExported;
        this.ui = ui;
    }        
    
    public String basePath()
    {
        return this.basePath;
    }
    
    /**
     * Create a new Folder in a {@link String} path
     * 
     * @param path Path of Alfresco folder
     */
    public void createFolder(String path, Date created, Date modified) throws Exception
    {
        path = this.basePath + path;
        log.debug("createFolder path to create : " + path);
       
        try
        {
            File dir = new File(path);
            if (!dir.exists())
            {
                if (!dir.mkdirs())
                {
                    log.error("createFolder failed to create path : " + path);
                }
                else
                {
                    log.debug("createFolder path : " + path);
                }
            }

            Path pathf = Paths.get(dir.getAbsolutePath());
            Files.setAttribute(pathf, "basic:creationTime", FileTime.fromMillis(created.getTime()), NOFOLLOW_LINKS);
            Files.setAttribute(pathf, "basic:lastModifiedTime", FileTime.fromMillis(modified.getTime()), NOFOLLOW_LINKS);
        }
        catch (Exception e) 
        {
            e.printStackTrace();
			ui.getWriter().write(e.toString());
        }
    }
    
    
    /**
     * Create a new file in the {@link String} path 
     * 
     * @param filePath Path of file
     * @throws IOException
     */
    private void createFile (String filePath) throws Exception 
    {
        log.debug("createFile = " + filePath);

        File f=new File(filePath);
        
        try 
        {  
            if(!f.exists())
            {
              if (!f.getParentFile().exists())
              {
                  if (!f.getParentFile().mkdirs())
                  {
                      log.error("failed to create folder : " + f.getParentFile().getPath());
                  }
                  else
                  {
                      log.debug("created folder : " + f.getParentFile().getPath());
                  }
              }
              f.createNewFile();
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
			ui.getWriter().write(e.toString());
        }
        log.debug("createFile filepath done");
    }
    
    
    /**
     * Create XML File
     * 
     * @param filePath Path of file
     * @return {@link String} Name of file
     * @throws Exception
     */
    private String createXmlFile(String filePath) throws Exception
    {
        String fp = filePath + ".metadata.properties.xml";
        
        this.createFile(fp);
        
        return fp;
    }
    
    
    /**
     * create content file
     * 
     * @param content
     * @param filePath
     * @throws IOException
     */
    public void insertFileContent (ByteArrayOutputStream out, String filePath) throws Exception 
    {
        log.debug("insertFileContent");
        filePath = this.basePath + filePath;
        
        log.debug("insertFileContent filepath = " + filePath);
        if(this.isFileExist(filePath) && this.scapeExported)
        {
            log.debug("insertFileContent ignore file");
            return;
        }
        
        this.createFile(filePath);
        
        try 
        {
            FileOutputStream output = new FileOutputStream(filePath);
            output.write(out.toByteArray());
            output.flush();
            output.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    /**
     * construct full file path and make directory if it does not exist
     * 
     * @param filePath
     * @throws IOException
     */
    public String createFullPath(String filePath) throws Exception 
    {
        log.debug("createFullPath");
        filePath = this.basePath + filePath;
        
        log.debug("createFullPath filepath = " + filePath);
        if(this.isFileExist(filePath) && this.scapeExported)
        {
            log.debug("createFullPath ignore file");
            return filePath;
        }
       
        File f=new File(filePath);
        
        try 
        {  
            if(!f.exists())
            {
              if (!f.getParentFile().exists())
              {
                  if (!f.getParentFile().mkdirs())
                  {
                      log.error("failed to create folder : " + f.getParentFile().getPath());
                  }
                  else
                  {
                      log.debug("created folder : " + f.getParentFile().getPath());
                  }
              }
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
			ui.getWriter().write(e.toString());
        }

        return filePath;
    }
    
    
    /**
     * Insert Content Properties in the XML File
     * 
     * @param type The type of node
     * @param aspects The aspect {@link List} of node in {@link String} format
     * @param properties The properties {@link Map} of node in {@link String} format
     * @param filePath The path of file
     * @throws Exception
     */
    public void insertFileProperties(String name, String nodeRef, String type, List<String> aspects,Map<String, String> properties, String filePath) throws Exception
    {
        String relPath = filePath;
        String rootPath = "/Company Home/Sites/metadata/";
        filePath = this.basePath + filePath;
        String metadataFilePath = this.basePath + rootPath + name;


        if(this.isFileExist(filePath) && this.isFileExist(metadataFilePath + ".metadata.properties.xml") && this.scapeExported)
        {
            return;
        }
        
        
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n<properties>";
        String footer = "\n</properties>";
        
        String tType = "<entry key=\"type\">" + type + "</entry>";
        String tNodeRef = "<entry key=\"nodeRef\">" + nodeRef + "</entry>";
        String tPath = "<entry key=\"path\">" + relPath + "</entry>";
        String tAspect = "<entry key=\"aspects\">" + this.formatAspects(aspects) + "</entry>";

        String text = "\n\t" + tType + "\n\t" + tNodeRef + "\n\t" + tPath + "\n\t" + tAspect;

        Set<String> set = properties.keySet();
        
        for (String string : set) 
        {
            String key = string;
            String value = properties.get(key);
            
            
            value = this.formatProperty(value);
            
            
            text += "\n\t<entry key=\"" + key +"\">" + value + "</entry>";
        }
        
        try 
        {
            String fp = this.createXmlFile(metadataFilePath);
            File file = new File(fp);
            
//            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            
            StringBuilder builder = new StringBuilder();
            builder.append(header);
            builder.append(text);
            builder.append(footer);
            
            bw.write(builder.toString());
            bw.close();
            
            
//            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
//            out.append(header);
//            out.append(text);
//            out.append(footer);
//            
//            out.flush();
//            out.close();
            
            
            
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        
    }
    
    
    /**
     * Format aspects
     * 
     * @param aspects
     * @return
     */
    private String formatAspects(List<String> aspects)
    {
                
        String dado = "";
        
        boolean flag = false;
        for (String string : aspects) 
        {
            if(flag)
            {
                dado += ",";
            }
            
            dado += string;
            flag = true;
        }
        
        return dado;
    }
    
    
    /**
     * Method to replace special character to html code
     * 
     * @param value {@link String} value of field
     * @return {@link String}
     */
    private String formatProperty(String value)
    {        
        
        //format &
        value = value.replaceAll("&", "&amp;");
        //format < and >
        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        
        return value;
    }
    
    
    /**
     * Method to see if file already exists
     * 
     * @param path The {@link String} path of file 
     * @return {@link Boolean}
     */
    private boolean isFileExist(String path)
    {
        File f=new File(path);
        
        if(f.exists())
        {
          return true;
        }

        return false;
    }
    
    
}
