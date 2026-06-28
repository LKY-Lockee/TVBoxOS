package com.github.tvbox.osc.server

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * @author pj567
 * @date 2021/1/4
 */
object ShellUtils {
	const val COMMAND_SU: String = "su"
	const val COMMAND_SH: String = "sh"
	const val COMMAND_EXIT: String = "exit\n"
	const val COMMAND_LINE_END: String = "\n"

	/**
	 * 查看是否有了root权限
	 */
	fun checkRootPermission(): Boolean {
		return execCommand("echo root", isRoot = true, isNeedResultMsg = false).result == 0
	}

	/**
	 * 执行shell命令，默认返回结果
	 */
	fun execCommand(command: String, isRoot: Boolean): CommandResult {
		return execCommand(arrayOf(command), isRoot, true)
	}

	/**
	 * 执行shell命令，默认返回结果
	 */
	fun execCommand(commands: List<String>, isRoot: Boolean): CommandResult {
		return execCommand(commands.toTypedArray(), isRoot, true)
	}

	/**
	 * 执行shell命令，默认返回结果
	 */
	fun execCommand(commands: Array<String>, isRoot: Boolean): CommandResult {
		return execCommand(commands, isRoot, true)
	}

	/**
	 * Execute shell command
	 */
	fun execCommand(command: String, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
		return execCommand(arrayOf(command), isRoot, isNeedResultMsg)
	}

	/**
	 * Execute shell commands
	 */
	fun execCommand(commands: List<String>, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
		return execCommand(commands.toTypedArray(), isRoot, isNeedResultMsg)
	}

	/**
	 * Execute shell commands
	 *
	 * @return
	 * * If isNeedResultMsg is false, [CommandResult.successMsg] is null and [CommandResult.errorMsg] is null.
	 * * If [CommandResult.result] is -1, there maybe some exception.
	 */
	fun execCommand(commands: Array<String>, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
		var result = -1
		if (commands.isEmpty()) {
			return CommandResult(result, null, null)
		}
		var process: Process? = null
		var successResult: BufferedReader? = null
		var errorResult: BufferedReader? = null
		var successMsg: String? = null
		var errorMsg: String? = null
		try {
			process = Runtime.getRuntime().exec(if (isRoot) COMMAND_SU else COMMAND_SH)
			DataOutputStream(process.outputStream).use { os ->
				for (command in commands) {
					os.write(command.toByteArray())
					os.writeBytes(COMMAND_LINE_END)
					os.flush()
				}
				os.writeBytes(COMMAND_EXIT)
				os.flush()
			}
			result = process.waitFor()
			if (isNeedResultMsg) {
				successResult = BufferedReader(InputStreamReader(process.inputStream))
				errorResult = BufferedReader(InputStreamReader(process.errorStream))
				successMsg = successResult.use { it.readText() }
				errorMsg = errorResult.use { it.readText() }
			}
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			try {
				successResult?.close()
				errorResult?.close()
			} catch (e: IOException) {
				e.printStackTrace()
			}
			process?.destroy()
		}
		return CommandResult(result, successMsg, errorMsg)
	}

	/**
	 * 运行结果
	 *
	 * * [result] Result of command, 0 means normal,	else means error, same to execute in Linux shell
	 * * [successMsg] Success message of command result
	 * * [errorMsg] Error message of command result
	 * 
	 * [Trinea](http://www.trinea.cn)
	 * 
	 * 2013-5-16
	 */
	data class CommandResult(
		val result: Int,
		val successMsg: String? = null,
		val errorMsg: String? = null
	)
}
