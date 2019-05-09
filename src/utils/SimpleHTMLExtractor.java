package utils;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import gate.Factory;
import gate.Gate;
import gate.util.GateException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author UPF
 */
public class SimpleHTMLExtractor {
    
    
       public static String extractFromURL(String url) throws IOException {
        String text="";
        Document doc;
        doc = (Document) Jsoup.connect(url).get();
        String title;
        System.out.println(doc.title());
        Elements els;
       
        els=doc.getElementsByTag("div");
        String txt;
        for(Element ele: els) {
             if(ele.hasAttr("itemprop")) {
                if(ele.attributes().get("itemprop").equals("articleBody")) {
                    txt=ele.text();
                    
                   text=text+ " " + txt;
                }
        }
            
        }
        return text;
    }
    
    public static void main(String[] args) throws IOException  {
        
      String text=extractFromURL("http://www.elmundo.es/internacional/2017/11/15/5a0c423b268e3eeb718b45ca.html");
      try {
          
          Gate.init();
          gate.Document doc;
          doc=Factory.newDocument(text);
          System.out.println(doc.getContent());
      } catch(GateException ge) {
          ge.printStackTrace();
      }    
               
        
      
    }
}

    