package Index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;

public class IndexFiles {
	private static final String INDEXPATH = "index";
	private static final String DOCPATH = "pageText";

	public void CreateIndex() throws IOException {
		final Path docDir = Paths.get(DOCPATH);
		if (!Files.isReadable(docDir)) {
			System.out.println("The directory " + docDir.toAbsolutePath() + " is not readable");
			System.exit(1);
		}
		Directory dir = FSDirectory.open(Paths.get(INDEXPATH));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);
		indexDocs(writer, docDir);
		writer.close();
	}
	private static void indexDocs(final IndexWriter writer, Path path) throws IOException{
		if(Files.isDirectory(path)){
			Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException{
					try{
						indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
					}catch (IOException ignore){
						
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}else{
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}
	private static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException{
		try (InputStream stream = Files.newInputStream(file)) {
			Document doc = new Document();
			Field pathField = new StringField("path", file.toString(), Field.Store.YES);
			doc.add(pathField);
			doc.add(new LongPoint("modified", lastModified));
			doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				System.out.println("adding " + file);
				writer.addDocument(doc);
			}else{
				System.out.println("updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}
		}
	}
	public static void main(String[] args) throws IOException{
		IndexFiles inf = new IndexFiles();
		inf.CreateIndex();
	}
}
