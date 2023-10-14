import pandas as pd
import numpy as np
import os

pat1 = """
// AUTO GENERATED CODE DO NOT EDIT
package decode

import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
"""
pat2 = """
class InstContorlDecoder extends Module {
  val table = TruthTable(
    Map(
"""


df = pd.read_csv(
    os.path.join(os.environ["NPC_HOME"], "playground/scripts/codemap.csv"), dtype=str
)
df.fillna(df.iloc[0], inplace=True)  # 默认值填充空白
defaultDict = df.iloc[0].to_dict()
df.replace("any", np.NaN, inplace=True)  # 把所有any改成NaN
df = df.drop(0, axis=0)

df_new = df.iloc[:, :3].copy()
# 编码字典
codemap = []


# 用于将数字转换成二进制字符串，如果是-1则转换成"?"
def num_to_bin(x):
    if x == -1:
        return "?"
    else:
        return bin(x)[2:]


# 编码
for colIndex in range(3, len(df.columns)):
    colName = df.columns[colIndex]
    values = set(df[colName].unique())
    # 如果只有 yes no，那就不用做map了
    if values == {"yes", "no"}:
        codemap.append((colName, []))
        df_new[colName] = df[colName].replace({np.NaN: "?", "yes": "1", "no": "0"})
    else:
        # 对字符串进行编码枚举
        codes, values = pd.factorize(df.iloc[:, colIndex])
        df_new[colName] = [num_to_bin(val) for val in codes]
        codemap.append((colName, list(values)))
    # 对df的每一列应用一个lambda函数，用于计算该列的最大位宽
    max_width = max(len(x) for x in df_new[colName])
    # 对df的所有元素应用一个lambda函数，用于根据最大位宽进行补齐或截断
    df_new[colName] = [
        x.zfill(max_width) if x != "?" else "?" * max_width for x in df_new[colName]
    ]
df_new["concat"] = df_new.apply(lambda row: " ".join(row[3:]), axis=1)


def genEnum(colName: str, values: 'list[str]'):
    if len(values) == 0:
        return ""
    # TODO: yesno -> 10
    ret = f"object {colName} extends ChiselEnum {'{'}\n"
    for index, value in enumerate(values):
        ret = ret + f"\tval {value} = Value({index}.U)\n"
    return ret + "}\n"


def genIOClass():
    ret = "class DecodeControlOut extends Bundle {\n"
    for colName, _ in codemap:
        ret += f"\tval {colName.lower()} = Output({ 'Bool()' if len(df_new[colName].iloc[0]) == 1 else f'UInt({len(df_new[colName].iloc[0])}.W)'})\n"
    return ret + "}\n"


def genIOObject():
    ret = """
object DecodeControlOut{
  def default() = {
    val defaultout = Wire(new DecodeControlOut);
"""
    for colName, _ in codemap:
        defaultValue = defaultDict[colName]
        if len(df_new[colName].iloc[0]) == 1:
            ret += f"\t defaultout.{colName.lower()} := {'true.B' if defaultValue == 'yes' else 'false.B'}\n"
        else:
            ret += f"\t defaultout.{colName.lower()} := {f'{colName}.{defaultValue}.asUInt' if defaultValue != 'any' else '0.U'}\n"
    return ret + "defaultout\n}\n}\n"


with open(
    os.path.join(
        os.environ["NPC_HOME"], "playground/src/decode/InstContorlDecoder.scala"
    ),
    "w",
) as f:
    f.write(pat1)
    f.writelines([genEnum(colName, values) for (colName, values) in codemap])
    f.write(genIOClass())
    f.write(genIOObject())
    f.write(pat2)

    f.writelines(
        df_new.loc[df_new["name"] != "inv"].apply(
            lambda row: f"\t\t  BitPat(\"b{row['code']}\") -> BitPat(\"b{row['concat']}\"), // {row['name']}\n",
            axis=1,
        )
    )
    f.write(
        f"\t),\n    BitPat(\"b{df_new.loc[df_new['name'] == 'inv', 'concat'].iloc[0]}\") // inv\n  )\n"
    )
    f.write("  val input  = IO(Input(UInt(32.W)))\n")
    f.write("  val output  = IO(new DecodeControlOut)\n")
    f.write("  val decodeOut = decoder(input, table)\n")
    offset = 0
    for colName, _ in reversed(codemap):
        keylen = len(df_new[colName].iloc[0])
        f.write(
            f"  output.{colName.lower()} := decodeOut({offset + keylen - 1}, {offset})\n"
        )
        offset += keylen
    f.write("}")
