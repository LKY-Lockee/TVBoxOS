package com.github.tvbox.osc.util

import com.github.tvbox.osc.bean.IpScanningVo
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Queue
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.min

class IpScanning {
	/**
	 * 线程数
	 */
	private val corePoolSize = 5

	/**
	 * 最大线程数
	 */
	private val maximumPoolSize = 10

	private val threadPool = ThreadPoolExecutor(
		corePoolSize, maximumPoolSize, 3,
		TimeUnit.NANOSECONDS, LinkedBlockingQueue(10),
		ThreadPoolExecutor.CallerRunsPolicy()
	)

	/**
	 * 通过IP扫描对应网段中可以使用的网段
	 *
	 * @param ips 输入的IP
	 */
	fun search(ips: String, all: Boolean): List<IpScanningVo> {
		val divisionIp = ips.lastIndexOf(".")
		val substring = ips.substring(0, divisionIp + 1)

		val last = ips.substring(divisionIp + 1)
		var end = last.toInt() + 30 // 搜索范围不是全部，缩小范围
		end = min(end, 255)
		if (all) end = 255
		val total = end

		// 扫描对应网段中的所有Ip
		val queue: BlockingQueue<IpScanningVo> = ArrayBlockingQueue(total)
		for (i in 1..<total) {
			val iip = substring + i
			threadPool.submit(PingIp(iip, queue))
		}
		threadPool.shutdown()
		// 判断当前线程是否全部执行完成,防止没有执行完返回结果
		while (!threadPool.isTerminated) {
		}
		return ArrayList(queue)
	}

	private data class PingIp(val ip: String, val array: Queue<IpScanningVo>) : Runnable {
		override fun run() {
			// 遍历IP地址
			var addIp: InetAddress? = null
			try {
				addIp = InetAddress.getByName(ip)
			} catch (e: UnknownHostException) {
				e.printStackTrace()
			}
			// 检查设备是否在线，其中1000ms指定的是超时时间
			// 当返回值是true时，说明host是可用的，false则不可。
			var status = false
			try {
				if (addIp != null) {
					status = addIp.isReachable(1000)
				}
			} catch (e: IOException) {
				e.printStackTrace()
			}
			if (status) {
				val resolvedIp = addIp ?: return
				val ipScanning = IpScanningVo(resolvedIp.hostName, ip)
				TVBoxRuntimeLog.i("IP地址为:$ip\t\t设备名称为: ${resolvedIp.hostName}\t\t是否可用: 可用")
				array.add(ipScanning)
			}
		}
	}
}
