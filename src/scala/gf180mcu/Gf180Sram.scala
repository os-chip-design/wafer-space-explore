package gf180mcu

import chisel3._
import chisel3.util.log2Ceil

/**
 * Ports shared by all GF180MCU SRAM macros: 8-bit words, all controls active low.
 *
 * VDD/VSS are left out, as in chip_core.sv without USE_POWER_PINS.
 * LibreLane connects them with PDN_MACRO_CONNECTIONS.
 */
class Gf180SramIO(words: Int) extends Bundle {
  val CLK = Input(Clock())
  val CEN = Input(Bool())     // Chip enable (active low)
  val GWEN = Input(Bool())    // Global write enable (active low)
  val WEN = Input(UInt(8.W))  // Bit write enable (active low)
  val A = Input(UInt(log2Ceil(words).W))
  val D = Input(UInt(8.W))
  val Q = Output(UInt(8.W))
}

/** Black box for the GF180MCU SRAM macro macroName with the given number of words. */
abstract class Gf180Sram(macroName: String, val words: Int) extends BlackBox {
  val io = IO(new Gf180SramIO(words))
  override def desiredName = macroName
}

// 5V SRAMs (SRAM=gf180mcu_fd_ip_sram)
class FdSram64x8 extends Gf180Sram("gf180mcu_fd_ip_sram__sram64x8m8wm1", 64)
class FdSram128x8 extends Gf180Sram("gf180mcu_fd_ip_sram__sram128x8m8wm1", 128)
class FdSram256x8 extends Gf180Sram("gf180mcu_fd_ip_sram__sram256x8m8wm1", 256)
class FdSram512x8 extends Gf180Sram("gf180mcu_fd_ip_sram__sram512x8m8wm1", 512)
