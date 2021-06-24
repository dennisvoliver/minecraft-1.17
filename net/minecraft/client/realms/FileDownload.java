package net.minecraft.client.realms;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.realms.dto.WorldDownload;
import net.minecraft.client.realms.exception.RealmsDefaultUncaughtExceptionHandler;
import net.minecraft.client.realms.gui.screen.RealmsDownloadLatestWorldScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class FileDownload {
   static final Logger LOGGER = LogManager.getLogger();
   volatile boolean cancelled;
   volatile boolean finished;
   volatile boolean error;
   volatile boolean extracting;
   private volatile File backupFile;
   volatile File resourcePackPath;
   private volatile HttpGet httpRequest;
   private Thread currentThread;
   private final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).build();
   private static final String[] INVALID_FILE_NAMES = new String[]{"CON", "COM", "PRN", "AUX", "CLOCK$", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

   public long contentLength(String downloadLink) {
      CloseableHttpClient closeableHttpClient = null;
      HttpGet httpGet = null;

      long var5;
      try {
         httpGet = new HttpGet(downloadLink);
         closeableHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
         CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpGet);
         var5 = Long.parseLong(closeableHttpResponse.getFirstHeader("Content-Length").getValue());
         return var5;
      } catch (Throwable var16) {
         LOGGER.error("Unable to get content length for download");
         var5 = 0L;
      } finally {
         if (httpGet != null) {
            httpGet.releaseConnection();
         }

         if (closeableHttpClient != null) {
            try {
               closeableHttpClient.close();
            } catch (IOException var15) {
               LOGGER.error((String)"Could not close http client", (Throwable)var15);
            }
         }

      }

      return var5;
   }

   public void downloadWorld(WorldDownload download, String message, RealmsDownloadLatestWorldScreen.DownloadStatus status, LevelStorage storage) {
      if (this.currentThread == null) {
         this.currentThread = new Thread(() -> {
            CloseableHttpClient closeableHttpClient = null;
            boolean var90 = false;

            label1408: {
               CloseableHttpResponse httpResponse4;
               FileOutputStream outputStream4;
               FileDownload.DownloadCountingOutputStream downloadCountingOutputStream4;
               FileDownload.ResourcePackProgressListener resourcePackProgressListener3;
               label1402: {
                  try {
                     var90 = true;
                     this.backupFile = File.createTempFile("backup", ".tar.gz");
                     this.httpRequest = new HttpGet(download.downloadLink);
                     closeableHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
                     httpResponse4 = closeableHttpClient.execute(this.httpRequest);
                     status.totalBytes = Long.parseLong(httpResponse4.getFirstHeader("Content-Length").getValue());
                     if (httpResponse4.getStatusLine().getStatusCode() != 200) {
                        this.error = true;
                        this.httpRequest.abort();
                        var90 = false;
                        break label1408;
                     }

                     outputStream4 = new FileOutputStream(this.backupFile);
                     FileDownload.ProgressListener progressListener = new FileDownload.ProgressListener(message.trim(), this.backupFile, storage, status);
                     downloadCountingOutputStream4 = new FileDownload.DownloadCountingOutputStream(outputStream4);
                     downloadCountingOutputStream4.setListener(progressListener);
                     IOUtils.copy((InputStream)httpResponse4.getEntity().getContent(), (OutputStream)downloadCountingOutputStream4);
                     var90 = false;
                     break label1402;
                  } catch (Exception var103) {
                     LOGGER.error((String)"Caught exception while downloading: {}", (Object)var103.getMessage());
                     this.error = true;
                     var90 = false;
                  } finally {
                     if (var90) {
                        this.httpRequest.releaseConnection();
                        if (this.backupFile != null) {
                           this.backupFile.delete();
                        }

                        if (!this.error) {
                           if (!download.resourcePackUrl.isEmpty() && !download.resourcePackHash.isEmpty()) {
                              try {
                                 this.backupFile = File.createTempFile("resources", ".tar.gz");
                                 this.httpRequest = new HttpGet(download.resourcePackUrl);
                                 HttpResponse httpResponse5 = closeableHttpClient.execute(this.httpRequest);
                                 status.totalBytes = Long.parseLong(httpResponse5.getFirstHeader("Content-Length").getValue());
                                 if (httpResponse5.getStatusLine().getStatusCode() != 200) {
                                    this.error = true;
                                    this.httpRequest.abort();
                                    return;
                                 }

                                 OutputStream outputStream5 = new FileOutputStream(this.backupFile);
                                 FileDownload.ResourcePackProgressListener resourcePackProgressListener4 = new FileDownload.ResourcePackProgressListener(this.backupFile, status, download);
                                 FileDownload.DownloadCountingOutputStream downloadCountingOutputStream5 = new FileDownload.DownloadCountingOutputStream(outputStream5);
                                 downloadCountingOutputStream5.setListener(resourcePackProgressListener4);
                                 IOUtils.copy((InputStream)httpResponse5.getEntity().getContent(), (OutputStream)downloadCountingOutputStream5);
                              } catch (Exception var95) {
                                 LOGGER.error((String)"Caught exception while downloading: {}", (Object)var95.getMessage());
                                 this.error = true;
                              } finally {
                                 this.httpRequest.releaseConnection();
                                 if (this.backupFile != null) {
                                    this.backupFile.delete();
                                 }

                              }
                           } else {
                              this.finished = true;
                           }
                        }

                        if (closeableHttpClient != null) {
                           try {
                              closeableHttpClient.close();
                           } catch (IOException var91) {
                              LOGGER.error("Failed to close Realms download client");
                           }
                        }

                     }
                  }

                  this.httpRequest.releaseConnection();
                  if (this.backupFile != null) {
                     this.backupFile.delete();
                  }

                  if (!this.error) {
                     if (!download.resourcePackUrl.isEmpty() && !download.resourcePackHash.isEmpty()) {
                        try {
                           this.backupFile = File.createTempFile("resources", ".tar.gz");
                           this.httpRequest = new HttpGet(download.resourcePackUrl);
                           httpResponse4 = closeableHttpClient.execute(this.httpRequest);
                           status.totalBytes = Long.parseLong(httpResponse4.getFirstHeader("Content-Length").getValue());
                           if (httpResponse4.getStatusLine().getStatusCode() != 200) {
                              this.error = true;
                              this.httpRequest.abort();
                              return;
                           }

                           outputStream4 = new FileOutputStream(this.backupFile);
                           resourcePackProgressListener3 = new FileDownload.ResourcePackProgressListener(this.backupFile, status, download);
                           downloadCountingOutputStream4 = new FileDownload.DownloadCountingOutputStream(outputStream4);
                           downloadCountingOutputStream4.setListener(resourcePackProgressListener3);
                           IOUtils.copy((InputStream)httpResponse4.getEntity().getContent(), (OutputStream)downloadCountingOutputStream4);
                        } catch (Exception var99) {
                           LOGGER.error((String)"Caught exception while downloading: {}", (Object)var99.getMessage());
                           this.error = true;
                        } finally {
                           this.httpRequest.releaseConnection();
                           if (this.backupFile != null) {
                              this.backupFile.delete();
                           }

                        }
                     } else {
                        this.finished = true;
                     }
                  }

                  if (closeableHttpClient != null) {
                     try {
                        closeableHttpClient.close();
                     } catch (IOException var93) {
                        LOGGER.error("Failed to close Realms download client");
                     }

                     return;
                  }

                  return;
               }

               this.httpRequest.releaseConnection();
               if (this.backupFile != null) {
                  this.backupFile.delete();
               }

               if (!this.error) {
                  if (!download.resourcePackUrl.isEmpty() && !download.resourcePackHash.isEmpty()) {
                     try {
                        this.backupFile = File.createTempFile("resources", ".tar.gz");
                        this.httpRequest = new HttpGet(download.resourcePackUrl);
                        httpResponse4 = closeableHttpClient.execute(this.httpRequest);
                        status.totalBytes = Long.parseLong(httpResponse4.getFirstHeader("Content-Length").getValue());
                        if (httpResponse4.getStatusLine().getStatusCode() != 200) {
                           this.error = true;
                           this.httpRequest.abort();
                           return;
                        }

                        outputStream4 = new FileOutputStream(this.backupFile);
                        resourcePackProgressListener3 = new FileDownload.ResourcePackProgressListener(this.backupFile, status, download);
                        downloadCountingOutputStream4 = new FileDownload.DownloadCountingOutputStream(outputStream4);
                        downloadCountingOutputStream4.setListener(resourcePackProgressListener3);
                        IOUtils.copy((InputStream)httpResponse4.getEntity().getContent(), (OutputStream)downloadCountingOutputStream4);
                     } catch (Exception var101) {
                        LOGGER.error((String)"Caught exception while downloading: {}", (Object)var101.getMessage());
                        this.error = true;
                     } finally {
                        this.httpRequest.releaseConnection();
                        if (this.backupFile != null) {
                           this.backupFile.delete();
                        }

                     }
                  } else {
                     this.finished = true;
                  }
               }

               if (closeableHttpClient != null) {
                  try {
                     closeableHttpClient.close();
                  } catch (IOException var94) {
                     LOGGER.error("Failed to close Realms download client");
                  }
               }

               return;
            }

            this.httpRequest.releaseConnection();
            if (this.backupFile != null) {
               this.backupFile.delete();
            }

            if (!this.error) {
               if (!download.resourcePackUrl.isEmpty() && !download.resourcePackHash.isEmpty()) {
                  try {
                     this.backupFile = File.createTempFile("resources", ".tar.gz");
                     this.httpRequest = new HttpGet(download.resourcePackUrl);
                     HttpResponse httpResponse2 = closeableHttpClient.execute(this.httpRequest);
                     status.totalBytes = Long.parseLong(httpResponse2.getFirstHeader("Content-Length").getValue());
                     if (httpResponse2.getStatusLine().getStatusCode() != 200) {
                        this.error = true;
                        this.httpRequest.abort();
                        return;
                     }

                     OutputStream outputStream = new FileOutputStream(this.backupFile);
                     FileDownload.ResourcePackProgressListener resourcePackProgressListener = new FileDownload.ResourcePackProgressListener(this.backupFile, status, download);
                     FileDownload.DownloadCountingOutputStream downloadCountingOutputStream = new FileDownload.DownloadCountingOutputStream(outputStream);
                     downloadCountingOutputStream.setListener(resourcePackProgressListener);
                     IOUtils.copy((InputStream)httpResponse2.getEntity().getContent(), (OutputStream)downloadCountingOutputStream);
                  } catch (Exception var97) {
                     LOGGER.error((String)"Caught exception while downloading: {}", (Object)var97.getMessage());
                     this.error = true;
                  } finally {
                     this.httpRequest.releaseConnection();
                     if (this.backupFile != null) {
                        this.backupFile.delete();
                     }

                  }
               } else {
                  this.finished = true;
               }
            }

            if (closeableHttpClient != null) {
               try {
                  closeableHttpClient.close();
               } catch (IOException var92) {
                  LOGGER.error("Failed to close Realms download client");
               }
            }

         });
         this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
         this.currentThread.start();
      }
   }

   public void cancel() {
      if (this.httpRequest != null) {
         this.httpRequest.abort();
      }

      if (this.backupFile != null) {
         this.backupFile.delete();
      }

      this.cancelled = true;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isError() {
      return this.error;
   }

   public boolean isExtracting() {
      return this.extracting;
   }

   public static String findAvailableFolderName(String folder) {
      folder = folder.replaceAll("[\\./\"]", "_");
      String[] var1 = INVALID_FILE_NAMES;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         String string = var1[var3];
         if (folder.equalsIgnoreCase(string)) {
            folder = "_" + folder + "_";
         }
      }

      return folder;
   }

   void untarGzipArchive(String name, File archive, LevelStorage storage) throws IOException {
      Pattern pattern = Pattern.compile(".*-([0-9]+)$");
      int i = 1;
      char[] var7 = SharedConstants.INVALID_CHARS_LEVEL_NAME;
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         char c = var7[var9];
         name = name.replace(c, '_');
      }

      if (StringUtils.isEmpty(name)) {
         name = "Realm";
      }

      name = findAvailableFolderName(name);

      try {
         Iterator var47 = storage.getLevelList().iterator();

         while(var47.hasNext()) {
            LevelSummary levelSummary = (LevelSummary)var47.next();
            if (levelSummary.getName().toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
               Matcher matcher = pattern.matcher(levelSummary.getName());
               if (matcher.matches()) {
                  if (Integer.valueOf(matcher.group(1)) > i) {
                     i = Integer.valueOf(matcher.group(1));
                  }
               } else {
                  ++i;
               }
            }
         }
      } catch (Exception var46) {
         LOGGER.error((String)"Error getting level list", (Throwable)var46);
         this.error = true;
         return;
      }

      String string2;
      if (storage.isLevelNameValid(name) && i <= 1) {
         string2 = name;
      } else {
         string2 = name + (i == 1 ? "" : "-" + i);
         if (!storage.isLevelNameValid(string2)) {
            boolean bl = false;

            while(!bl) {
               ++i;
               string2 = name + (i == 1 ? "" : "-" + i);
               if (storage.isLevelNameValid(string2)) {
                  bl = true;
               }
            }
         }
      }

      TarArchiveInputStream tarArchiveInputStream = null;
      File file = new File(MinecraftClient.getInstance().runDirectory.getAbsolutePath(), "saves");
      boolean var32 = false;

      LevelStorage.Session session2;
      Path path2;
      label453: {
         try {
            var32 = true;
            file.mkdir();
            tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(archive))));

            for(TarArchiveEntry tarArchiveEntry = tarArchiveInputStream.getNextTarEntry(); tarArchiveEntry != null; tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) {
               File file2 = new File(file, tarArchiveEntry.getName().replace("world", string2));
               if (tarArchiveEntry.isDirectory()) {
                  file2.mkdirs();
               } else {
                  file2.createNewFile();
                  FileOutputStream fileOutputStream = new FileOutputStream(file2);

                  try {
                     IOUtils.copy((InputStream)tarArchiveInputStream, (OutputStream)fileOutputStream);
                  } catch (Throwable var37) {
                     try {
                        fileOutputStream.close();
                     } catch (Throwable var35) {
                        var37.addSuppressed(var35);
                     }

                     throw var37;
                  }

                  fileOutputStream.close();
               }
            }

            var32 = false;
            break label453;
         } catch (Exception var44) {
            LOGGER.error((String)"Error extracting world", (Throwable)var44);
            this.error = true;
            var32 = false;
         } finally {
            if (var32) {
               if (tarArchiveInputStream != null) {
                  tarArchiveInputStream.close();
               }

               if (archive != null) {
                  archive.delete();
               }

               try {
                  LevelStorage.Session session3 = storage.createSession(string2);

                  try {
                     session3.save(string2.trim());
                     Path path3 = session3.getDirectory(WorldSavePath.LEVEL_DAT);
                     readNbtFile(path3.toFile());
                  } catch (Throwable var38) {
                     if (session3 != null) {
                        try {
                           session3.close();
                        } catch (Throwable var33) {
                           var38.addSuppressed(var33);
                        }
                     }

                     throw var38;
                  }

                  if (session3 != null) {
                     session3.close();
                  }
               } catch (IOException var39) {
                  LOGGER.error((String)"Failed to rename unpacked realms level {}", (Object)string2, (Object)var39);
               }

               this.resourcePackPath = new File(file, string2 + File.separator + "resources.zip");
            }
         }

         if (tarArchiveInputStream != null) {
            tarArchiveInputStream.close();
         }

         if (archive != null) {
            archive.delete();
         }

         try {
            session2 = storage.createSession(string2);

            try {
               session2.save(string2.trim());
               path2 = session2.getDirectory(WorldSavePath.LEVEL_DAT);
               readNbtFile(path2.toFile());
            } catch (Throwable var40) {
               if (session2 != null) {
                  try {
                     session2.close();
                  } catch (Throwable var34) {
                     var40.addSuppressed(var34);
                  }
               }

               throw var40;
            }

            if (session2 != null) {
               session2.close();
            }
         } catch (IOException var41) {
            LOGGER.error((String)"Failed to rename unpacked realms level {}", (Object)string2, (Object)var41);
         }

         this.resourcePackPath = new File(file, string2 + File.separator + "resources.zip");
         return;
      }

      if (tarArchiveInputStream != null) {
         tarArchiveInputStream.close();
      }

      if (archive != null) {
         archive.delete();
      }

      try {
         session2 = storage.createSession(string2);

         try {
            session2.save(string2.trim());
            path2 = session2.getDirectory(WorldSavePath.LEVEL_DAT);
            readNbtFile(path2.toFile());
         } catch (Throwable var42) {
            if (session2 != null) {
               try {
                  session2.close();
               } catch (Throwable var36) {
                  var42.addSuppressed(var36);
               }
            }

            throw var42;
         }

         if (session2 != null) {
            session2.close();
         }
      } catch (IOException var43) {
         LOGGER.error((String)"Failed to rename unpacked realms level {}", (Object)string2, (Object)var43);
      }

      this.resourcePackPath = new File(file, string2 + File.separator + "resources.zip");
   }

   private static void readNbtFile(File file) {
      if (file.exists()) {
         try {
            NbtCompound nbtCompound = NbtIo.readCompressed(file);
            NbtCompound nbtCompound2 = nbtCompound.getCompound("Data");
            nbtCompound2.remove("Player");
            NbtIo.writeCompressed(nbtCompound, file);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }

   }

   @Environment(EnvType.CLIENT)
   class ResourcePackProgressListener implements ActionListener {
      private final File tempFile;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;
      private final WorldDownload worldDownload;

      ResourcePackProgressListener(File file, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, WorldDownload worldDownload) {
         this.tempFile = file;
         this.downloadStatus = downloadStatus;
         this.worldDownload = worldDownload;
      }

      public void actionPerformed(ActionEvent e) {
         this.downloadStatus.bytesWritten = ((FileDownload.DownloadCountingOutputStream)e.getSource()).getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled) {
            try {
               String string = Hashing.sha1().hashBytes(Files.toByteArray(this.tempFile)).toString();
               if (string.equals(this.worldDownload.resourcePackHash)) {
                  FileUtils.copyFile(this.tempFile, FileDownload.this.resourcePackPath);
                  FileDownload.this.finished = true;
               } else {
                  FileDownload.LOGGER.error((String)"Resourcepack had wrong hash (expected {}, found {}). Deleting it.", (Object)this.worldDownload.resourcePackHash, (Object)string);
                  FileUtils.deleteQuietly(this.tempFile);
                  FileDownload.this.error = true;
               }
            } catch (IOException var3) {
               FileDownload.LOGGER.error((String)"Error copying resourcepack file: {}", (Object)var3.getMessage());
               FileDownload.this.error = true;
            }
         }

      }
   }

   @Environment(EnvType.CLIENT)
   private class DownloadCountingOutputStream extends CountingOutputStream {
      private ActionListener listener;

      public DownloadCountingOutputStream(OutputStream out) {
         super(out);
      }

      public void setListener(ActionListener listener) {
         this.listener = listener;
      }

      protected void afterWrite(int n) throws IOException {
         super.afterWrite(n);
         if (this.listener != null) {
            this.listener.actionPerformed(new ActionEvent(this, 0, (String)null));
         }

      }
   }

   @Environment(EnvType.CLIENT)
   class ProgressListener implements ActionListener {
      private final String worldName;
      private final File tempFile;
      private final LevelStorage levelStorageSource;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

      ProgressListener(String worldName, File tempFile, LevelStorage levelStorageSource, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus) {
         this.worldName = worldName;
         this.tempFile = tempFile;
         this.levelStorageSource = levelStorageSource;
         this.downloadStatus = downloadStatus;
      }

      public void actionPerformed(ActionEvent e) {
         this.downloadStatus.bytesWritten = ((FileDownload.DownloadCountingOutputStream)e.getSource()).getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled && !FileDownload.this.error) {
            try {
               FileDownload.this.extracting = true;
               FileDownload.this.untarGzipArchive(this.worldName, this.tempFile, this.levelStorageSource);
            } catch (IOException var3) {
               FileDownload.LOGGER.error((String)"Error extracting archive", (Throwable)var3);
               FileDownload.this.error = true;
            }
         }

      }
   }
}
