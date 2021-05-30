export class SmsDto {
    constructor(
        public commandId: number,
        public userId: number,
        public message: string,
        public results: Array<SmsDto.Result>,
    ) {
    }
}

export namespace SmsDto {
    export class Result {
        constructor(
            public phone: string,
            public status: string,
        ) {
        }
    }
}

export class SendSmsForm {
    constructor(
        public username: string = '',
        public password: string = '',
        public message: string = '',
        public phones: string = ''
    ) {
    }
}
