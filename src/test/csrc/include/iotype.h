#ifndef __IO_TYPE_H
#define __IO_TYPE_H

struct VecInfo {
  uint8_t vstart; // 0-127
  uint8_t vl; // 0-128
  uint8_t vlmul; // only 3 bits
  bool vm; // 0: masked, 1: unmasked
  bool ta; // 0: undisturbed, 1: agnostic
  bool ma; // 0: undisturbed, 1: agnostic
};

struct VecInput {
  uint64_t src1[2];
  uint64_t src2[2];
  uint64_t src3[2];
  uint64_t src4[2];
  uint8_t fuType; // only 5bits(or 2bits?)
  uint8_t fuOpType;
  uint8_t uop_idx; // only 6 bits
  uint8_t sew; // only 2 bits
  bool src_widen;
  bool widen;
  uint8_t rm; // only 3 bits
  uint8_t rm_s; //only 2 bits
  VecInfo vinfo;
};

struct VecOutput {
  uint64_t result[2];
  uint32_t fflags[2]; // only 20bits for each op
  uint64_t vxsat;    // NOTE: The length of the aligned structure must be an integer multiple of the largest alignment parameter (PPB) in the member
};

struct VecInputE8 {
  uint8_t src1[16];
  uint8_t src2[16];
  uint8_t src3[16];
  uint8_t src4[16];
  uint8_t fuType; // only 5bits(or 2bits?)
  uint8_t fuOpType;
  uint8_t uop_idx; // only 6 bits
  uint8_t sew; // only 2 bits
  bool src_widen;
  bool widen;
  uint8_t rm; // only 3 bits
  uint8_t rm_s; //only 2 bits
  VecInfo vinfo;
};

struct VecInputE16 {
  uint16_t src1[8];
  uint16_t src2[8];
  uint16_t src3[8];
  uint16_t src4[8];
  uint8_t fuType; // only 5bits(or 2bits?)
  uint8_t fuOpType;
  uint8_t uop_idx; // only 6 bits
  uint8_t sew; // only 2 bits
  bool src_widen;
  bool widen;
  uint8_t rm; // only 3 bits
  uint8_t rm_s; //only 2 bits
  VecInfo vinfo;
};

struct VecInputE32 {
  uint32_t src1[4];
  uint32_t src2[4];
  uint32_t src3[4];
  uint32_t src4[4];
  uint8_t fuType; // only 5bits(or 2bits?)
  uint8_t fuOpType;
  uint8_t uop_idx; // only 6 bits
  uint8_t sew; // only 2 bits
  bool src_widen;
  bool widen;
  uint8_t rm; // only 3 bits
  uint8_t rm_s; //only 2 bits
  VecInfo vinfo;
};

struct VecOutputE8 {
  uint8_t result[16];
  // uint8_t fflags[2]; // only 20bits for each op
  uint8_t vxsat[16];
};

struct VecOutputE16 {
  uint16_t result[8];
  uint8_t fflags[8]; // only 20bits for each op
  uint8_t vxsat[8];
};

struct VecOutputE32 {
  uint32_t result[4];
  uint8_t fflags[4]; // only 20bits for each op
  uint8_t vxsat[4];
};



struct ElementOutput {
  uint64_t result;
  uint8_t fflags;
  bool vxsat;
};

struct ElementInput {
  uint64_t src1;
  uint64_t src2;
  uint64_t src3;
  uint64_t src4;
  uint8_t fuOpType;
  bool src_widen;
  bool widen;
  uint8_t uop_idx;
  uint8_t rm;
  uint8_t rm_s;
};


struct VSlideUpInput {
  uint64_t *src_data;
  uint64_t *prev_data;
  uint16_t mask;
  uint64_t slide;
  int mask_start_idx;
  int slide_base;
  int elements;
  VecInfo *vinfo;
};

#endif