import chisel3._
import chisel3.experimental.Analog
import chisel3.util.{Cat, Fill}
import scopt.OParser

import gf180mcu._

import scala.collection.immutable.ListMap

/**
 * Chisel version of src/chip_core.sv, always with the 5V SRAMs.
 *
 * The Verilog parameters become constructor parameters.
 */
class ChipCore(numInputPads: Int, numBidirPads: Int, numAnalogPads: Int) extends Module {

  val input_in = IO(Input(UInt(numInputPads.W)))   // Input value
  val input_pu = IO(Output(UInt(numInputPads.W)))  // Pull-up
  val input_pd = IO(Output(UInt(numInputPads.W)))  // Pull-down

  val bidir_in = IO(Input(UInt(numBidirPads.W)))   // Input value
  val bidir_out = IO(Output(UInt(numBidirPads.W))) // Output value
  val bidir_oe = IO(Output(UInt(numBidirPads.W)))  // Output enable
  val bidir_cs = IO(Output(UInt(numBidirPads.W)))  // Input type (0=CMOS Buffer, 1=Schmitt Trigger)
  val bidir_sl = IO(Output(UInt(numBidirPads.W)))  // Slew rate (0=fast, 1=slow)
  val bidir_ie = IO(Output(UInt(numBidirPads.W)))  // Input enable
  val bidir_pu = IO(Output(UInt(numBidirPads.W)))  // Pull-up
  val bidir_pd = IO(Output(UInt(numBidirPads.W)))  // Pull-down

  val analog = IO(Analog(numAnalogPads.W))         // Analog

  // Disable pull-up and pull-down for input
  input_pu := 0.U
  input_pd := 0.U

  // Set the bidir as output
  val allOnes = ((BigInt(1) << numBidirPads) - 1).U(numBidirPads.W)
  bidir_oe := allOnes
  bidir_cs := 0.U
  bidir_sl := 0.U
  bidir_ie := ~bidir_oe
  bidir_pu := 0.U
  bidir_pd := 0.U

  // Same example logic as chip_core.sv: count while all inputs are high
  val countReg = RegInit(0.U(numBidirPads.W))
  when(input_in.andR) {
    countReg := countReg + 1.U
  }

  // The instance names sram_0 and sram_1 must match the macro placement in librelane/macros
  val sram_0 = Module(new FdSram512x8)
  val sram_1 = Module(new FdSram512x8)
  for (s <- Seq(sram_0, sram_1)) {
    s.io.CLK := clock
    s.io.CEN := false.B
    s.io.GWEN := input_in.andR
    s.io.WEN := Fill(8, true.B)
    s.io.A := countReg(8, 0)
    s.io.D := countReg(16, 9)
  }

  bidir_out := countReg ^ Cat(sram_0.io.Q, sram_1.io.Q)
}

object ChipCore extends App {
  // Pad counts (input, bidir, analog) per slot, from src/slot_defines.svh
  val slots = ListMap(
    "1x1" -> (12, 40, 2),
    "0p5x1" -> (4, 44, 6),
    "1x0p5" -> (4, 46, 4),
    "0p5x0p5" -> (4, 38, 4),
  )

  // add more configuration here
  case class Config(slot: String = "1x1")

  val builder = OParser.builder[Config]
  val parser = {
    import builder._
    OParser.sequence(
      programName("ChipCore"),
      head("Generates generated/ChipCore.v"),
      help("help").text("print this usage text"),
      opt[String]("slot")
        .valueName(slots.keys.mkString("|"))
        .action((x, c) => c.copy(slot = x))
        .validate(x =>
          if (slots.contains(x)) success
          else failure(s"unknown slot $x, known slots: ${slots.keys.mkString(", ")}"))
        .text("slot size (default: 1x1)"),
    )
  }

  OParser.parse(parser, args, Config()) match {
    case Some(config) =>
      val (numInput, numBidir, numAnalog) = slots(config.slot)
      emitVerilog(new ChipCore(numInput, numBidir, numAnalog), Array("--target-dir", "generated"))
    case None =>
      // scopt has already printed the error
      sys.exit(1)
  }
}
