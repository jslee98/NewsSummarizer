/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package callSUMMA;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import gate.*;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.OffsetComparator;
import gate.util.persistence.PersistenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jeffrey Lee & UPF
 *
 * TODO: Reformat HTML output
 *       Possibly add more invalid features (embedded fb posts?)
 */
public class SummarizingRSS {

    public static String gappToTest = "TextSummarizerEN.gapp";
    public static CorpusController application;

    // News Feeds
    public static final String laVanguardia = "https://www.lavanguardia.com/mvc/feed/rss/home";
    public static final String elPais = "http://ep00.epimg.net/rss/tags/ultimas_noticias.xml";
    public static final String bbc = "http://feeds.bbci.co.uk/news/rss.xml?edition=int";
    public static final String theGuardian = "https://www.theguardian.com/world/rss";
    public static final String elConfidencial = "https://rss.elconfidencial.com/mundo/";

    public static String rssSite = theGuardian;


    public static void main(String[] args) {

        try {

            URL url;
            XmlReader reader = null;
            Document doc;
            Corpus corpus;

            OutputStreamWriter osw;
            File fout = new File("." + File.separator + "output" + File.separator + "The_Guardian_latest_news.html");
            String header = "<!DOCTYPE html>\n" + "<head>" + "<meta charset=\"UTF-8\">" + "</head>" + "<html>\n" +
                    "<body>\n" + "\n" + "<h1>Latest News From The Guardian </h1>\n";
            String footer = "</body>\n" + "</html>";
            FileOutputStream writer = new FileOutputStream(fout);
            osw = new OutputStreamWriter(writer, "utf-8");

            osw.append(header + "\n");
            osw.flush();

            String title;
            String link;

            try {
                Gate.init();

                // load the GAPP
                application = (CorpusController) PersistenceManager.loadObjectFromFile(new File("." + File.separator + "gapps" + File.separator + gappToTest));

                url = new URL(rssSite);
                reader = new XmlReader(url);

                SyndFeed feed = new SyndFeedInput().build(reader);

                System.out.println("Feed Title: " + feed.getTitle());
                //  The corpus to store the document
                corpus = Factory.newCorpus("");
                int count = 0;
                Iterator i = feed.getEntries().iterator();
                Document cleanDocument;
                while (i.hasNext() && count < 10) {
                    count++;
                    SyndEntry entry = (SyndEntry) i.next();
                    title = entry.getTitle();
                    SyndContent c;

                    link = entry.getLink();

                    System.out.println(title);
                    System.out.println(link);
                    doc = Factory.newDocument(new URL(link), "utf-8");

                    String[] articleInfo  = extractTextNews(doc);
                    String subtitle = articleInfo[0];
                    String author = articleInfo[1];
                    String date = articleInfo[2];
                    String articleContent = articleInfo[3];
                    String[] formattedText = createQuickInfo(subtitle, author, date);
                    articleContent = formattedText[1] + articleContent;

                    osw.append("<h2> <a href=" + link + ">" + title + "</a> </h2> \n");
                    osw.append(formattedText[0]);

                    cleanDocument = Factory.newDocument(articleContent);
                    corpus.add(cleanDocument);
                    application.setCorpus(corpus);
                    application.execute();
                    osw.append(getSummary(cleanDocument) + "\n");
                    osw.append(footer);
                    osw.flush();

                    System.out.println("*** SUMMARY ***");
                    System.out.println(getSummary(cleanDocument));
                    System.out.println("***************");

                    Factory.deleteResource(doc);
                    Factory.deleteResource(cleanDocument);
                }
                osw.close();

            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (FeedException ex) {
                ex.printStackTrace();
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            } catch (PersistenceException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ResourceInstantiationException ex) {
                ex.printStackTrace();
            } catch (GateException ge) {
                ge.printStackTrace();
            }

        } catch (IOException ex) {
            Logger.getLogger(SummarizingRSS.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static String[] extractTextNews(Document doc) {

        String subtitle = "";
        String author = "";
        String date = "";
        String text = "";
        String line;
        Annotation para;
        FeatureMap fMap;
        Long start, end;

        AnnotationSet paras = doc.getAnnotations("Original markups").get("p");
        String dc = doc.getContent().toString();
        ArrayList<Annotation> list = new ArrayList(paras);
        Collections.sort(list, new OffsetComparator());

        for (int i = 0; i < list.size(); i++) {
            para = list.get(i);
            fMap =  para.getFeatures();
            start = para.getStartNode().getOffset();
            end = para.getEndNode().getOffset();
            if (i == 0) {
                subtitle = dc.substring(start.intValue(), end.intValue());
            } else if (i == 1) {
                author = dc.substring(start.intValue(), end.intValue());
            } else if (i ==2) {
                date = dc.substring(start.intValue(), end.intValue());
            } else if (validateFeatureMap(fMap)) {
                start = para.getStartNode().getOffset();
                end = para.getEndNode().getOffset();
                line = dc.substring(start.intValue(), end.intValue());
                text = text + "\n" + line;
            }

        }

        return new String[]{subtitle, author, date, text};
    }

    public static String[] createQuickInfo(String subtitle, String author, String date) {
        String res = "<h3>";
        if (author.indexOf("GMT") > -1 && author.indexOf("2019") > -1) {
            if (author.indexOf("Last modified") > -1) {
                int start = author.indexOf("Last modified") + 17;
                author = author.substring(start);
            }
            if (author.indexOf("First published") > -1) {
                int end = author.indexOf("First published");
                author = author.substring(0,end);
            }
            return new String[]{ res + author + "</h3>", date };
        } else {
            int index  = author.indexOf(",");
            boolean stop = false;
            while (index >= 0 && !stop) {
                System.out.println(index);
                author = author.substring(0, index - 1) + author.substring(index);
                int nextIndex = author.indexOf(",", index);
                if (nextIndex == -1) {
                    stop = true;
                }
            }
            if (date.indexOf("Last modified") > -1) {
                int start = date.indexOf("Last modified") + 17;
                date = date.substring(start);
            }
            if (date.indexOf("First published") > -1) {
                int end = date.indexOf("First published");
                date = date.substring(0,end);
            }
            return new String[]{res + author + ": " + date + "</h3>", ""};
        }
    }

    public static boolean validateFeatureMap(FeatureMap fMap) {
        String[] invalidParaClasses = {"tweet-body", "block-time"};
        if (fMap.isEmpty()) {
            return true;
        } else {
            for (String invalid : invalidParaClasses) {
                if (fMap.containsValue(invalid)){
                    //System.out.println("Invalid Feature Found in Text");
                    return false;
                }
            }
            // For The Guardian, it seems as though only unclassed paragraphs contain actual text.
            return false;
        }

    }

    public static String getSummary(Document doc) {
        String summary = "";
        String dc = doc.getContent().toString();
        AnnotationSet sentences = doc.getAnnotations("EXTRACT").get("Sentence");
        // sort the annotations
        Annotation sentence;
        Long start, end;
        ArrayList<Annotation> sentList = new ArrayList(sentences);
        Collections.sort(sentList, new OffsetComparator());
        for (int s = 0; s < sentList.size(); s++) {
            sentence = sentList.get(s);
            start = sentence.getStartNode().getOffset();
            end = sentence.getEndNode().getOffset();
            summary = summary + dc.substring(start.intValue(), end.intValue()) + "\n";
        }
        return summary;
    }

}
