package yunsuan.vector

import chisel3._
import chisel3.util._
import yunsuan.util._
import yunsuan.VectorElementFormat
import yunsuan.vector.vpermutil._


// vcompress.vm
class Compress(n: Int) extends VPermModule {
    val io = IO(new VPermBundle() {
        val os_base   = Input(UInt(8.W)) // ones_sum_base
        val pmos      = Input(UInt(8.W)) // previous_mask_ones_sum

        val vl_valid  = Input(UInt(8.W))
        val ta        = Input(Bool())

        val mask      = Input(UInt((VLEN/8).W))
        val src_data  = Input(UInt(VLEN.W))
        val prev_data = Input(UInt(VLEN.W))

        val res_data  = Output(UInt(VLEN.W))
        val cmos_last = Output(UInt(8.W)) // current_mask_ones_sum
    })

    val src_data_vec  = Wire(Vec(n, UInt((VLEN/n).W)))
    val prev_data_vec = Wire(Vec(n, UInt((VLEN/n).W)))
    val res_data_vec  = Wire(Vec(n, UInt((VLEN/n).W)))
    val cmos_vec      = Wire(Vec(n, UInt(8.W)))

    for(i <- 0 until n) {
        src_data_vec(i)  := io.src_data((VLEN/n)*(i+1)-1, (VLEN/n)*i)
        prev_data_vec(i) := io.prev_data((VLEN/n)*(i+1)-1, (VLEN/n)*i)

        val elements_idx = io.os_base +& i.U
        val res_keep_old_vd = (elements_idx >= io.vl_valid) && !io.ta
        val res_agnostic = (elements_idx >= io.vl_valid) && io.ta

        when (res_keep_old_vd) {
            res_data_vec(i) := prev_data_vec(i)
        }.elsewhen (res_agnostic) {
            res_data_vec(i) := Fill(VLEN/n, 1.U(1.W))
        }.otherwise {
            res_data_vec(i) := prev_data_vec(i)
        }
    }

    for(i <- 0 until n) {
        if (i == 0)
            cmos_vec(i) := io.pmos + io.mask(i)
        else
            cmos_vec(i) := cmos_vec(i-1) + io.mask(i)
    }

    for(i <- 0 until n) {
        val res_idx = cmos_vec(i) + ~io.os_base // = cmos_vec(i) - base - 1
    
        when( io.mask(i).asBool && (io.os_base < cmos_vec(i)) && (cmos_vec(i) < (io.os_base +& (n+1).U)) && (cmos_vec(i) <= io.vl_valid) ) {
            res_data_vec(res_idx) := src_data_vec(i)
        }
    }

    io.res_data  := res_data_vec.reduce{ (a, b) => Cat(b, a) }
    io.cmos_last := cmos_vec(n-1)
}

class CompressModule extends VPermModule {
    val io = IO(new VPermIO)

    val vformat = io.vd_type(1,0)
    //val sew          = LookupTree(vformat, VFormat.VFormatTable.map(p => (p._1, p._2._1)))
    val elem_num     = LookupTree(vformat, VFormat.VFormatTable.map(p => (p._1, p._2._2)))
    val elem_num_pow = LookupTree(vformat, VFormat.VFormatTable.map(p => (p._1, p._2._3)))

    val vs1_idx = Wire(UInt(3.W))
    when ( io.uop_idx === 0.U || io.uop_idx === 1.U ) {
        vs1_idx := 0.U
    }.elsewhen ( (2.U <= io.uop_idx) && (io.uop_idx <= 4.U) ) {
        vs1_idx := 1.U
    }.elsewhen ( (5.U <= io.uop_idx) && (io.uop_idx <= 8.U) ) {
        vs1_idx := 2.U
    }.elsewhen ( (9.U <= io.uop_idx) && (io.uop_idx <= 13.U) ) {
        vs1_idx := 3.U
    }.elsewhen ( (14.U <= io.uop_idx) && (io.uop_idx <= 19.U) ) {
        vs1_idx := 4.U
    }.elsewhen ( (20.U <= io.uop_idx) && (io.uop_idx <= 26.U) ) {
        vs1_idx := 5.U
    }.elsewhen ( (27.U <= io.uop_idx) && (io.uop_idx <= 34.U) ) {
        vs1_idx := 6.U
    }.otherwise {
        vs1_idx := 7.U
    }
    val mask_start_idx = vs1_idx << elem_num_pow

    val vd_idx = Wire(UInt(3.W))
    when ( io.uop_idx === 42.U ) {
        vd_idx := 7.U
    }.elsewhen ( (io.uop_idx === 33.U) || (io.uop_idx === 41.U) ) {
        vd_idx := 6.U
    }.elsewhen ( (io.uop_idx === 25.U) || (io.uop_idx === 32.U) || (io.uop_idx === 40.U) ) {
        vd_idx := 5.U
    }.elsewhen ( (io.uop_idx === 18.U) || (io.uop_idx === 24.U) || (io.uop_idx === 31.U) || (io.uop_idx === 39.U) ) {
        vd_idx := 4.U
    }.elsewhen ( (io.uop_idx === 12.U) || (io.uop_idx === 17.U) || (io.uop_idx === 23.U) || (io.uop_idx === 30.U) || (io.uop_idx === 38.U) ) {
        vd_idx := 3.U
    }.elsewhen ( (io.uop_idx === 7.U) || (io.uop_idx === 11.U) || (io.uop_idx === 16.U) || (io.uop_idx === 22.U) || (io.uop_idx === 29.U) || (io.uop_idx === 37.U) ) {
        vd_idx := 2.U
    }.elsewhen ( (io.uop_idx === 3.U) || (io.uop_idx === 6.U) || (io.uop_idx === 10.U) || (io.uop_idx === 15.U) || (io.uop_idx === 21.U) || (io.uop_idx === 28.U) || (io.uop_idx === 36.U) ) {
        vd_idx := 1.U
    }.otherwise {
        vd_idx := 0.U
    }
    val ones_sum_base = vd_idx << elem_num_pow

    val select_mask = SelectMaskN(io.vs1, 16, mask_start_idx)
    val vs1_mask = LookupTree(vformat, List(
        VectorElementFormat.b -> select_mask,
        VectorElementFormat.h -> ZeroExt(select_mask(7,0), 16),
        VectorElementFormat.w -> ZeroExt(select_mask(3,0), 16),
        VectorElementFormat.d -> ZeroExt(select_mask(1,0), 16)
    ))

    val vlmax = LookupTree(io.vlmul, List(
        "b000".U -> elem_num,          //lmul=1
        "b001".U -> (elem_num << 1),   //lmul=2
        "b010".U -> (elem_num << 2),   //lmul=4
        "b011".U -> (elem_num << 3),   //lmul=8
        "b101".U -> (elem_num >> 3),   //lmul=1/8
        "b110".U -> (elem_num >> 2),   //lmul=1/4
        "b111".U -> (elem_num >> 1)    //lmul=1/2
    ))
    val vl_valid = Mux(io.vl <= vlmax, io.vl, vlmax)

    val compress_module_0 = Module(new Compress(16)) //sew=8
    val compress_module_1 = Module(new Compress(8))  //sew=16
    val compress_module_2 = Module(new Compress(4))  //sew=32
    val compress_module_3 = Module(new Compress(2))  //sew=64

    val compress_module = VecInit(Seq(compress_module_0.io, compress_module_1.io, compress_module_2.io, compress_module_3.io))
    for(i <- 0 until 4) {
        compress_module(i).os_base   := ones_sum_base
        compress_module(i).pmos      := io.mask(7,0)
        compress_module(i).vl_valid  := vl_valid
        compress_module(i).ta        := io.ta
        compress_module(i).mask      := vs1_mask
        compress_module(i).src_data  := io.vs2
        compress_module(i).prev_data := io.old_vd
    }

    val compress_res_data = LookupTree(vformat, List(
        VectorElementFormat.b -> compress_module_0.io.res_data,
        VectorElementFormat.h -> compress_module_1.io.res_data,
        VectorElementFormat.w -> compress_module_2.io.res_data,
        VectorElementFormat.d -> compress_module_3.io.res_data
    ))
    val ones_sum_res_data = LookupTree(vformat, List(
        VectorElementFormat.b -> compress_module_0.io.cmos_last,
        VectorElementFormat.h -> compress_module_1.io.cmos_last,
        VectorElementFormat.w -> compress_module_2.io.cmos_last,
        VectorElementFormat.d -> compress_module_3.io.cmos_last
    ))

    val output_mask_ones_sum = (io.uop_idx === 1.U)

    io.res_vd := Mux(io.vstart >= io.vl, io.old_vd, Mux(output_mask_ones_sum, ZeroExt(ones_sum_res_data, VLEN), compress_res_data))
}
