/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.common.util

import java.io.File
import java.lang.reflect.Method
import java.net.{URL, URLClassLoader}

object ClassLoaderUtils extends Logger {

  private val classloader = ClassLoader.getSystemClassLoader.asInstanceOf[URLClassLoader]

  /**
   * 指定 classLoader执行代码...
   *
   * @param targetClassLoader
   * @param func
   * @tparam R
   * @return
   */
  def runAsClassLoader[R](targetClassLoader: ClassLoader, func: () => R): R = {
    val originalClassLoader = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(targetClassLoader)
      func()
    } catch {
      case e: Exception => throw e
    } finally {
      Thread.currentThread.setContextClassLoader(originalClassLoader)
    }
  }

  def loadJar(jarFilePath: String): Unit = {
    val jarFile = new File(jarFilePath)
    require(jarFile.exists, s"[StreamX] jarFilePath:$jarFilePath is not exists")
    require(jarFile.isFile, s"[StreamX] jarFilePath:$jarFilePath is not file")
    loadPath(jarFile.getAbsolutePath, List(".jar", ".zip"))
  }

  def loadJars(path: String): Unit = {
    val jarDir = new File(path)
    require(jarDir.exists, s"[StreamX] jarPath: $path is not exists")
    require(jarDir.isDirectory, s"[StreamX] jarPath: $path is not directory")
    require(jarDir.listFiles.length > 0, s"[StreamX] have not jar in path:$path")
    jarDir.listFiles.foreach { x =>
      loadPath(x.getAbsolutePath, List(".jar", ".zip"))
    }
  }


  def loadResource(filepath: String): Unit = {
    val file = new File(filepath)
    addURL(file)
  }

  def loadResourceDir(filepath: String): Unit = {
    val file = new File(filepath)
    loopDirs(file)
  }

  /**
   * URLClassLoader的addURL方法
   */
  private val addURL: Method = try {
    val add = classOf[URLClassLoader].getDeclaredMethod("addURL", Array(classOf[URL]): _*)
    add.setAccessible(true)
    add
  } catch {
    case e: Exception => throw new RuntimeException(e)
  }

  private[this] def loadPath(filepath: String, ext: List[String]): Unit = {
    val file = new File(filepath)
    loopFiles(file, ext)
  }


  private[this] def loopDirs(file: File): Unit = { // 资源文件只加载路径
    if (file.isDirectory) {
      addURL(file)
      file.listFiles.foreach(loopDirs)
    }
  }


  private[this] def loopFiles(file: File, ext: List[String] = List()): Unit = {
    if (file.isDirectory) {
      file.listFiles.foreach(x => loopFiles(x, ext))
    } else {
      if (ext.isEmpty) {
        addURL(file)
      } else if (ext.exists(x => file.getName.endsWith(x))) {
        Utils.checkJarFile(file.toURI.toURL)
        addURL(file)
      }
    }
  }

  private[this] def addURL(file: File): Unit = {
    try {
      addURL.invoke(classloader, file.toURI.toURL)
    } catch {
      case e: Exception =>
    }
  }


}
